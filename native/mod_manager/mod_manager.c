/*
 *  mod_cluster
 *
 *  Copyright(c) 2008 Red Hat Middleware, LLC,
 *  and individual contributors as indicated by the @authors tag.
 *  See the copyright.txt in the distribution for a
 *  full listing of individual contributors. 
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library in the file COPYING.LIB;
 *  if not, write to the Free Software Foundation, Inc.,
 *  59 Temple Place - Suite 330, Boston, MA 02111-1307, USA
 *
 * @author Jean-Frederic Clere
 * @version $Revision$
 */

#include "apr_strings.h"
#include "apr_lib.h"
#include "apr_uuid.h"

#define CORE_PRIVATE
#include "httpd.h"
#include "http_config.h"
#include "http_log.h"
#include "http_main.h"
#include "http_request.h"
#include "http_protocol.h"
#include "http_core.h"
#include "scoreboard.h"
#include "mod_proxy.h"
#include "ap_mpm.h"

#include "mod_proxy_cluster.h"

#include "slotmem.h"

#include "node.h"
#include "host.h"
#include "context.h"
#include "balancer.h"
#include "sessionid.h"
#include "domain.h"
#include "jgroupsid.h"

#define DEFMAXCONTEXT   100
#define DEFMAXNODE      20
#define DEFMAXHOST      20
#define DEFMAXSESSIONID 0 /* it has performance/security impact */
#define DEFMAXJGROUPSID  0
#define MAXMESSSIZE     1024

/* Error messages */
#define TYPESYNTAX 1
#define SMESPAR "SYNTAX: Can't parse message"
#define SBALBIG "SYNTAX: Balancer field too big"
#define SBAFBIG "SYNTAX: A field is too big"
#define SROUBIG "SYNTAX: JVMRoute field too big"
#define SROUBAD "SYNTAX: JVMRoute can't be empty"
#define SDOMBIG "SYNTAX: LBGroup field too big"
#define SHOSBIG "SYNTAX: Host field too big"
#define SPORBIG "SYNTAX: Port field too big"    
#define STYPBIG "SYNTAX: Type field too big"
#define SALIBAD "SYNTAX: Alias without Context"
#define SCONBAD "SYNTAX: Context without Alias"
#define SBADFLD "SYNTAX: Invalid field \"%s\" in message"
#define SMISFLD "SYNTAX: Mandatory field(s) missing in message"
#define SCMDUNS "SYNTAX: Command is not supported"
#define SMULALB "SYNTAX: Only one Alias in APP command"
#define SMULCTB "SYNTAX: Only one Context in APP command"
#define SREADER "SYNTAX: %s can't read POST data"

#define SJIDBIG "SYNTAX: JGroupUuid field too big"
#define SJDDBIG "SYNTAX: JGroupData field too big"
#define SJIDBAD "SYNTAX: JGroupUuid can't be empty"

#define TYPEMEM 2
#define MNODEUI "MEM: Can't update or insert node"
#define MNODERM "MEM: Old node still exist"
#define MBALAUI "MEM: Can't update or insert balancer"
#define MNODERD "MEM: Can't read node"
#define MHOSTRD "MEM: Can't read host alias"
#define MHOSTUI "MEM: Can't update or insert host alias"
#define MCONTUI "MEM: Can't update or insert context"
#define MJBIDRD "MEM: Can't read JGroupId"
#define MJBIDUI "MEM: Can't update or insert JGroupId"

/* Protocol version supported */
#define VERSION_PROTOCOL "0.2.1"

/* Internal substitution for node commands */
#define NODE_COMMAND "/NODE_COMMAND"

/* range of the commands */
#define RANGECONTEXT 0
#define RANGENODE    1
#define RANGEDOMAIN  2

/* define HAVE_CLUSTER_EX_DEBUG to have extented debug in mod_cluster */
#define HAVE_CLUSTER_EX_DEBUG 0

/* css for ui */
#define BOOTSTRAP3_CSS "/*! normalize.css v3.0.0 | MIT License | git.io/normalize */html{font-family:sans-serif;-ms-text-size-adjust:100%;-webkit-text-size-adjust:100%}body{margin:0}article,aside,details,figcaption,figure,footer,header,hgroup,main,nav,section,summary{display:block}audio,canvas,progress,video{display:inline-block;vertical-align:baseline}audio:not([controls]){display:none;height:0}[hidden],template{display:none}a{background:0 0}a:active,a:hover{outline:0}abbr[title]{border-bottom:1px dotted}b,strong{font-weight:700}dfn{font-style:italic}h1{font-size:2em;margin:.67em 0}mark{background:#ff0;color:#000}small{font-size:80%}sub,sup{font-size:75%;line-height:0;position:relative;vertical-align:baseline}sup{top:-.5em}sub{bottom:-.25em}img{border:0}svg:not(:root){overflow:hidden}figure{margin:1em 40px}hr{-moz-box-sizing:content-box;box-sizing:content-box;height:0}pre{overflow:auto}code,kbd,pre,samp{font-family:monospace,monospace;font-size:1em}button,input,optgroup,select,textarea{color:inherit;font:inherit;margin:0}button{overflow:visible}button,select{text-transform:none}button,html input[type=button],input[type=reset],input[type=submit]{-webkit-appearance:button;cursor:pointer}button[disabled],html input[disabled]{cursor:default}button::-moz-focus-inner,input::-moz-focus-inner{border:0;padding:0}input{line-height:normal}input[type=checkbox],input[type=radio]{box-sizing:border-box;padding:0}input[type=number]::-webkit-inner-spin-button,input[type=number]::-webkit-outer-spin-button{height:auto}input[type=search]{-webkit-appearance:textfield;-moz-box-sizing:content-box;-webkit-box-sizing:content-box;box-sizing:content-box}input[type=search]::-webkit-search-cancel-button,input[type=search]::-webkit-search-decoration{-webkit-appearance:none}fieldset{border:1px solid silver;margin:0 2px;padding:.35em .625em .75em}legend{border:0;padding:0}textarea{overflow:auto}optgroup{font-weight:700}table{border-collapse:collapse;border-spacing:0}td,th{padding:0}@media print{*{text-shadow:none!important;color:#000!important;background:transparent!important;box-shadow:none!important}a,a:visited{text-decoration:underline}a[href]:after{content:\" (\" attr(href) \")\"}abbr[title]:after{content:\" (\" attr(title) \")\"}a[href^=\"javascript:\"]:after,a[href^=\"#\"]:after{content:\"\"}pre,blockquote{border:1px solid #999;page-break-inside:avoid}thead{display:table-header-group}tr,img{page-break-inside:avoid}img{max-width:100%!important}p,h2,h3{orphans:3;widows:3}h2,h3{page-break-after:avoid}select{background:#fff!important}.navbar{display:none}.table td,.table th{background-color:#fff!important}.btn>.caret,.dropup>.btn>.caret{border-top-color:#000!important}.label{border:1px solid #000}.table{border-collapse:collapse!important}.table-bordered th,.table-bordered td{border:1px solid #ddd!important}}*{-webkit-box-sizing:border-box;-moz-box-sizing:border-box;box-sizing:border-box}:before,:after{-webkit-box-sizing:border-box;-moz-box-sizing:border-box;box-sizing:border-box}html{font-size:62.5%;-webkit-tap-highlight-color:rgba(0,0,0,0)}body{font-family:\"Helvetica Neue\",Helvetica,Arial,sans-serif;font-size:14px;line-height:1.42857143;color:#333;background-color:#fff}input,button,select,textarea{font-family:inherit;font-size:inherit;line-height:inherit}a{color:#428bca;text-decoration:none}a:hover,a:focus{color:#2a6496;text-decoration:underline}a:focus{outline:thin dotted;outline:5px auto -webkit-focus-ring-color;outline-offset:-2px}figure{margin:0}img{vertical-align:middle}.img-responsive,.thumbnail>img,.thumbnail a>img,.carousel-inner>.item>img,.carousel-inner>.item>a>img{display:block;max-width:100%;height:auto}.img-rounded{border-radius:6px}.img-thumbnail{padding:4px;line-height:1.42857143;background-color:#fff;border:1px solid #ddd;border-radius:4px;-webkit-transition:all .2s ease-in-out;transition:all .2s ease-in-out;display:inline-block;max-width:100%;height:auto}.img-circle{border-radius:50%}hr{margin-top:20px;margin-bottom:20px;border:0;border-top:1px solid #eee}.sr-only{position:absolute;width:1px;height:1px;margin:-1px;padding:0;overflow:hidden;clip:rect(0,0,0,0);border:0}h1,h2,h3,h4,h5,h6,.h1,.h2,.h3,.h4,.h5,.h6{font-family:inherit;font-weight:500;line-height:1.1;color:inherit}h1 small,h2 small,h3 small,h4 small,h5 small,h6 small,.h1 small,.h2 small,.h3 small,.h4 small,.h5 small,.h6 small,h1 .small,h2 .small,h3 .small,h4 .small,h5 .small,h6 .small,.h1 .small,.h2 .small,.h3 .small,.h4 .small,.h5 .small,.h6 .small{font-weight:400;line-height:1;color:#999}h1,.h1,h2,.h2,h3,.h3{margin-top:20px;margin-bottom:10px}h1 small,.h1 small,h2 small,.h2 small,h3 small,.h3 small,h1 .small,.h1 .small,h2 .small,.h2 .small,h3 .small,.h3 .small{font-size:65%}h4,.h4,h5,.h5,h6,.h6{margin-top:10px;margin-bottom:10px}h4 small,.h4 small,h5 small,.h5 small,h6 small,.h6 small,h4 .small,.h4 .small,h5 .small,.h5 .small,h6 .small,.h6 .small{font-size:75%}h1,.h1{font-size:36px}h2,.h2{font-size:30px}h3,.h3{font-size:24px}h4,.h4{font-size:18px}h5,.h5{font-size:14px}h6,.h6{font-size:12px}p{margin:0 0 10px}.lead{margin-bottom:20px;font-size:16px;font-weight:200;line-height:1.4}@media (min-width:768px){.lead{font-size:21px}}small,.small{font-size:85%}cite{font-style:normal}.text-left{text-align:left}.text-right{text-align:right}.text-center{text-align:center}.text-justify{text-align:justify}.text-muted{color:#999}.text-primary{color:#428bca}a.text-primary:hover{color:#3071a9}.text-success{color:#3c763d}a.text-success:hover{color:#2b542c}.text-info{color:#31708f}a.text-info:hover{color:#245269}.text-warning{color:#8a6d3b}a.text-warning:hover{color:#66512c}.text-danger{color:#a94442}a.text-danger:hover{color:#843534}.bg-primary{color:#fff;background-color:#428bca}a.bg-primary:hover{background-color:#3071a9}.bg-success{background-color:#dff0d8}a.bg-success:hover{background-color:#c1e2b3}.bg-info{background-color:#d9edf7}a.bg-info:hover{background-color:#afd9ee}.bg-warning{background-color:#fcf8e3}a.bg-warning:hover{background-color:#f7ecb5}.bg-danger{background-color:#f2dede}a.bg-danger:hover{background-color:#e4b9b9}.page-header{padding-bottom:9px;margin:40px 0 20px;border-bottom:1px solid #eee}ul,ol{margin-top:0;margin-bottom:10px}ul ul,ol ul,ul ol,ol ol{margin-bottom:0}.list-unstyled{padding-left:0;list-style:none}.list-inline{padding-left:0;list-style:none;margin-left:-5px}.list-inline>li{display:inline-block;padding-left:5px;padding-right:5px}dl{margin-top:0;margin-bottom:20px}dt,dd{line-height:1.42857143}dt{font-weight:700}dd{margin-left:0}@media (min-width:768px){.dl-horizontal dt{float:left;width:160px;clear:left;text-align:right;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}.dl-horizontal dd{margin-left:180px}}abbr[title],abbr[data-original-title]{cursor:help;border-bottom:1px dotted #999}.initialism{font-size:90%;text-transform:uppercase}blockquote{padding:10px 20px;margin:0 0 20px;font-size:17.5px;border-left:5px solid #eee}blockquote p:last-child,blockquote ul:last-child,blockquote ol:last-child{margin-bottom:0}blockquote footer,blockquote small,blockquote .small{display:block;font-size:80%;line-height:1.42857143;color:#999}blockquote footer:before,blockquote small:before,blockquote .small:before{content:'\\2014 \\00A0'}.blockquote-reverse,blockquote.pull-right{padding-right:15px;padding-left:0;border-right:5px solid #eee;border-left:0;text-align:right}.blockquote-reverse footer:before,blockquote.pull-right footer:before,.blockquote-reverse small:before,blockquote.pull-right small:before,.blockquote-reverse .small:before,blockquote.pull-right .small:before{content:''}.blockquote-reverse footer:after,blockquote.pull-right footer:after,.blockquote-reverse small:after,blockquote.pull-right small:after,.blockquote-reverse .small:after,blockquote.pull-right .small:after{content:'\\00A0 \\2014'}blockquote:before,blockquote:after{content:\"\"}address{margin-bottom:20px;font-style:normal;line-height:1.42857143}code,kbd,pre,samp{font-family:Menlo,Monaco,Consolas,\"Courier New\",monospace}code{padding:2px 4px;font-size:90%;color:#c7254e;background-color:#f9f2f4;white-space:nowrap;border-radius:4px}kbd{padding:2px 4px;font-size:90%;color:#fff;background-color:#333;border-radius:3px;box-shadow:inset 0 -1px 0 rgba(0,0,0,.25)}pre{display:block;padding:9.5px;margin:0 0 10px;font-size:13px;line-height:1.42857143;word-break:break-all;word-wrap:break-word;color:#333;background-color:#f5f5f5;border:1px solid #ccc;border-radius:4px}pre code{padding:0;font-size:inherit;color:inherit;white-space:pre-wrap;background-color:transparent;border-radius:0}.pre-scrollable{max-height:340px;overflow-y:scroll}.container{margin-right:auto;margin-left:auto;padding-left:15px;padding-right:15px}@media (min-width:768px){.container{width:750px}}@media (min-width:992px){.container{width:970px}}@media (min-width:1200px){.container{width:1170px}}.container-fluid{margin-right:auto;margin-left:auto;padding-left:15px;padding-right:15px}.row{margin-left:-15px;margin-right:-15px}.col-xs-1,.col-sm-1,.col-md-1,.col-lg-1,.col-xs-2,.col-sm-2,.col-md-2,.col-lg-2,.col-xs-3,.col-sm-3,.col-md-3,.col-lg-3,.col-xs-4,.col-sm-4,.col-md-4,.col-lg-4,.col-xs-5,.col-sm-5,.col-md-5,.col-lg-5,.col-xs-6,.col-sm-6,.col-md-6,.col-lg-6,.col-xs-7,.col-sm-7,.col-md-7,.col-lg-7,.col-xs-8,.col-sm-8,.col-md-8,.col-lg-8,.col-xs-9,.col-sm-9,.col-md-9,.col-lg-9,.col-xs-10,.col-sm-10,.col-md-10,.col-lg-10,.col-xs-11,.col-sm-11,.col-md-11,.col-lg-11,.col-xs-12,.col-sm-12,.col-md-12,.col-lg-12{position:relative;min-height:1px;padding-left:15px;padding-right:15px}.col-xs-1,.col-xs-2,.col-xs-3,.col-xs-4,.col-xs-5,.col-xs-6,.col-xs-7,.col-xs-8,.col-xs-9,.col-xs-10,.col-xs-11,.col-xs-12{float:left}.col-xs-12{width:100%}.col-xs-11{width:91.66666667%}.col-xs-10{width:83.33333333%}.col-xs-9{width:75%}.col-xs-8{width:66.66666667%}.col-xs-7{width:58.33333333%}.col-xs-6{width:50%}.col-xs-5{width:41.66666667%}.col-xs-4{width:33.33333333%}.col-xs-3{width:25%}.col-xs-2{width:16.66666667%}.col-xs-1{width:8.33333333%}.col-xs-pull-12{right:100%}.col-xs-pull-11{right:91.66666667%}.col-xs-pull-10{right:83.33333333%}.col-xs-pull-9{right:75%}.col-xs-pull-8{right:66.66666667%}.col-xs-pull-7{right:58.33333333%}.col-xs-pull-6{right:50%}.col-xs-pull-5{right:41.66666667%}.col-xs-pull-4{right:33.33333333%}.col-xs-pull-3{right:25%}.col-xs-pull-2{right:16.66666667%}.col-xs-pull-1{right:8.33333333%}.col-xs-pull-0{right:0}.col-xs-push-12{left:100%}.col-xs-push-11{left:91.66666667%}.col-xs-push-10{left:83.33333333%}.col-xs-push-9{left:75%}.col-xs-push-8{left:66.66666667%}.col-xs-push-7{left:58.33333333%}.col-xs-push-6{left:50%}.col-xs-push-5{left:41.66666667%}.col-xs-push-4{left:33.33333333%}.col-xs-push-3{left:25%}.col-xs-push-2{left:16.66666667%}.col-xs-push-1{left:8.33333333%}.col-xs-push-0{left:0}.col-xs-offset-12{margin-left:100%}.col-xs-offset-11{margin-left:91.66666667%}.col-xs-offset-10{margin-left:83.33333333%}.col-xs-offset-9{margin-left:75%}.col-xs-offset-8{margin-left:66.66666667%}.col-xs-offset-7{margin-left:58.33333333%}.col-xs-offset-6{margin-left:50%}.col-xs-offset-5{margin-left:41.66666667%}.col-xs-offset-4{margin-left:33.33333333%}.col-xs-offset-3{margin-left:25%}.col-xs-offset-2{margin-left:16.66666667%}.col-xs-offset-1{margin-left:8.33333333%}.col-xs-offset-0{margin-left:0}@media (min-width:768px){.col-sm-1,.col-sm-2,.col-sm-3,.col-sm-4,.col-sm-5,.col-sm-6,.col-sm-7,.col-sm-8,.col-sm-9,.col-sm-10,.col-sm-11,.col-sm-12{float:left}.col-sm-12{width:100%}.col-sm-11{width:91.66666667%}.col-sm-10{width:83.33333333%}.col-sm-9{width:75%}.col-sm-8{width:66.66666667%}.col-sm-7{width:58.33333333%}.col-sm-6{width:50%}.col-sm-5{width:41.66666667%}.col-sm-4{width:33.33333333%}.col-sm-3{width:25%}.col-sm-2{width:16.66666667%}.col-sm-1{width:8.33333333%}.col-sm-pull-12{right:100%}.col-sm-pull-11{right:91.66666667%}.col-sm-pull-10{right:83.33333333%}.col-sm-pull-9{right:75%}.col-sm-pull-8{right:66.66666667%}.col-sm-pull-7{right:58.33333333%}.col-sm-pull-6{right:50%}.col-sm-pull-5{right:41.66666667%}.col-sm-pull-4{right:33.33333333%}.col-sm-pull-3{right:25%}.col-sm-pull-2{right:16.66666667%}.col-sm-pull-1{right:8.33333333%}.col-sm-pull-0{right:0}.col-sm-push-12{left:100%}.col-sm-push-11{left:91.66666667%}.col-sm-push-10{left:83.33333333%}.col-sm-push-9{left:75%}.col-sm-push-8{left:66.66666667%}.col-sm-push-7{left:58.33333333%}.col-sm-push-6{left:50%}.col-sm-push-5{left:41.66666667%}.col-sm-push-4{left:33.33333333%}.col-sm-push-3{left:25%}.col-sm-push-2{left:16.66666667%}.col-sm-push-1{left:8.33333333%}.col-sm-push-0{left:0}.col-sm-offset-12{margin-left:100%}.col-sm-offset-11{margin-left:91.66666667%}.col-sm-offset-10{margin-left:83.33333333%}.col-sm-offset-9{margin-left:75%}.col-sm-offset-8{margin-left:66.66666667%}.col-sm-offset-7{margin-left:58.33333333%}.col-sm-offset-6{margin-left:50%}.col-sm-offset-5{margin-left:41.66666667%}.col-sm-offset-4{margin-left:33.33333333%}.col-sm-offset-3{margin-left:25%}.col-sm-offset-2{margin-left:16.66666667%}.col-sm-offset-1{margin-left:8.33333333%}.col-sm-offset-0{margin-left:0}}@media (min-width:992px){.col-md-1,.col-md-2,.col-md-3,.col-md-4,.col-md-5,.col-md-6,.col-md-7,.col-md-8,.col-md-9,.col-md-10,.col-md-11,.col-md-12{float:left}.col-md-12{width:100%}.col-md-11{width:91.66666667%}.col-md-10{width:83.33333333%}.col-md-9{width:75%}.col-md-8{width:66.66666667%}.col-md-7{width:58.33333333%}.col-md-6{width:50%}.col-md-5{width:41.66666667%}.col-md-4{width:33.33333333%}.col-md-3{width:25%}.col-md-2{width:16.66666667%}.col-md-1{width:8.33333333%}.col-md-pull-12{right:100%}.col-md-pull-11{right:91.66666667%}.col-md-pull-10{right:83.33333333%}.col-md-pull-9{right:75%}.col-md-pull-8{right:66.66666667%}.col-md-pull-7{right:58.33333333%}.col-md-pull-6{right:50%}.col-md-pull-5{right:41.66666667%}.col-md-pull-4{right:33.33333333%}.col-md-pull-3{right:25%}.col-md-pull-2{right:16.66666667%}.col-md-pull-1{right:8.33333333%}.col-md-pull-0{right:0}.col-md-push-12{left:100%}.col-md-push-11{left:91.66666667%}.col-md-push-10{left:83.33333333%}.col-md-push-9{left:75%}.col-md-push-8{left:66.66666667%}.col-md-push-7{left:58.33333333%}.col-md-push-6{left:50%}.col-md-push-5{left:41.66666667%}.col-md-push-4{left:33.33333333%}.col-md-push-3{left:25%}.col-md-push-2{left:16.66666667%}.col-md-push-1{left:8.33333333%}.col-md-push-0{left:0}.col-md-offset-12{margin-left:100%}.col-md-offset-11{margin-left:91.66666667%}.col-md-offset-10{margin-left:83.33333333%}.col-md-offset-9{margin-left:75%}.col-md-offset-8{margin-left:66.66666667%}.col-md-offset-7{margin-left:58.33333333%}.col-md-offset-6{margin-left:50%}.col-md-offset-5{margin-left:41.66666667%}.col-md-offset-4{margin-left:33.33333333%}.col-md-offset-3{margin-left:25%}.col-md-offset-2{margin-left:16.66666667%}.col-md-offset-1{margin-left:8.33333333%}.col-md-offset-0{margin-left:0}}@media (min-width:1200px){.col-lg-1,.col-lg-2,.col-lg-3,.col-lg-4,.col-lg-5,.col-lg-6,.col-lg-7,.col-lg-8,.col-lg-9,.col-lg-10,.col-lg-11,.col-lg-12{float:left}.col-lg-12{width:100%}.col-lg-11{width:91.66666667%}.col-lg-10{width:83.33333333%}.col-lg-9{width:75%}.col-lg-8{width:66.66666667%}.col-lg-7{width:58.33333333%}.col-lg-6{width:50%}.col-lg-5{width:41.66666667%}.col-lg-4{width:33.33333333%}.col-lg-3{width:25%}.col-lg-2{width:16.66666667%}.col-lg-1{width:8.33333333%}.col-lg-pull-12{right:100%}.col-lg-pull-11{right:91.66666667%}.col-lg-pull-10{right:83.33333333%}.col-lg-pull-9{right:75%}.col-lg-pull-8{right:66.66666667%}.col-lg-pull-7{right:58.33333333%}.col-lg-pull-6{right:50%}.col-lg-pull-5{right:41.66666667%}.col-lg-pull-4{right:33.33333333%}.col-lg-pull-3{right:25%}.col-lg-pull-2{right:16.66666667%}.col-lg-pull-1{right:8.33333333%}.col-lg-pull-0{right:0}.col-lg-push-12{left:100%}.col-lg-push-11{left:91.66666667%}.col-lg-push-10{left:83.33333333%}.col-lg-push-9{left:75%}.col-lg-push-8{left:66.66666667%}.col-lg-push-7{left:58.33333333%}.col-lg-push-6{left:50%}.col-lg-push-5{left:41.66666667%}.col-lg-push-4{left:33.33333333%}.col-lg-push-3{left:25%}.col-lg-push-2{left:16.66666667%}.col-lg-push-1{left:8.33333333%}.col-lg-push-0{left:0}.col-lg-offset-12{margin-left:100%}.col-lg-offset-11{margin-left:91.66666667%}.col-lg-offset-10{margin-left:83.33333333%}.col-lg-offset-9{margin-left:75%}.col-lg-offset-8{margin-left:66.66666667%}.col-lg-offset-7{margin-left:58.33333333%}.col-lg-offset-6{margin-left:50%}.col-lg-offset-5{margin-left:41.66666667%}.col-lg-offset-4{margin-left:33.33333333%}.col-lg-offset-3{margin-left:25%}.col-lg-offset-2{margin-left:16.66666667%}.col-lg-offset-1{margin-left:8.33333333%}.col-lg-offset-0{margin-left:0}}table{max-width:100%;background-color:transparent}th{text-align:left}.table{width:100%;margin-bottom:20px}.table>thead>tr>th,.table>tbody>tr>th,.table>tfoot>tr>th,.table>thead>tr>td,.table>tbody>tr>td,.table>tfoot>tr>td{padding:8px;line-height:1.42857143;vertical-align:top;border-top:1px solid #ddd}.table>thead>tr>th{vertical-align:bottom;border-bottom:2px solid #ddd}.table>caption+thead>tr:first-child>th,.table>colgroup+thead>tr:first-child>th,.table>thead:first-child>tr:first-child>th,.table>caption+thead>tr:first-child>td,.table>colgroup+thead>tr:first-child>td,.table>thead:first-child>tr:first-child>td{border-top:0}.table>tbody+tbody{border-top:2px solid #ddd}.table .table{background-color:#fff}.table-condensed>thead>tr>th,.table-condensed>tbody>tr>th,.table-condensed>tfoot>tr>th,.table-condensed>thead>tr>td,.table-condensed>tbody>tr>td,.table-condensed>tfoot>tr>td{padding:5px}.table-bordered{border:1px solid #ddd}.table-bordered>thead>tr>th,.table-bordered>tbody>tr>th,.table-bordered>tfoot>tr>th,.table-bordered>thead>tr>td,.table-bordered>tbody>tr>td,.table-bordered>tfoot>tr>td{border:1px solid #ddd}.table-bordered>thead>tr>th,.table-bordered>thead>tr>td{border-bottom-width:2px}.table-striped>tbody>tr:nth-child(odd)>td,.table-striped>tbody>tr:nth-child(odd)>th{background-color:#f9f9f9}.table-hover>tbody>tr:hover>td,.table-hover>tbody>tr:hover>th{background-color:#f5f5f5}table col[class*=col-]{position:static;float:none;display:table-column}table td[class*=col-],table th[class*=col-]{position:static;float:none;display:table-cell}.table>thead>tr>td.active,.table>tbody>tr>td.active,.table>tfoot>tr>td.active,.table>thead>tr>th.active,.table>tbody>tr>th.active,.table>tfoot>tr>th.active,.table>thead>tr.active>td,.table>tbody>tr.active>td,.table>tfoot>tr.active>td,.table>thead>tr.active>th,.table>tbody>tr.active>th,.table>tfoot>tr.active>th{background-color:#f5f5f5}.table-hover>tbody>tr>td.active:hover,.table-hover>tbody>tr>th.active:hover,.table-hover>tbody>tr.active:hover>td,.table-hover>tbody>tr.active:hover>th{background-color:#e8e8e8}.table>thead>tr>td.success,.table>tbody>tr>td.success,.table>tfoot>tr>td.success,.table>thead>tr>th.success,.table>tbody>tr>th.success,.table>tfoot>tr>th.success,.table>thead>tr.success>td,.table>tbody>tr.success>td,.table>tfoot>tr.success>td,.table>thead>tr.success>th,.table>tbody>tr.success>th,.table>tfoot>tr.success>th{background-color:#dff0d8}.table-hover>tbody>tr>td.success:hover,.table-hover>tbody>tr>th.success:hover,.table-hover>tbody>tr.success:hover>td,.table-hover>tbody>tr.success:hover>th{background-color:#d0e9c6}.table>thead>tr>td.info,.table>tbody>tr>td.info,.table>tfoot>tr>td.info,.table>thead>tr>th.info,.table>tbody>tr>th.info,.table>tfoot>tr>th.info,.table>thead>tr.info>td,.table>tbody>tr.info>td,.table>tfoot>tr.info>td,.table>thead>tr.info>th,.table>tbody>tr.info>th,.table>tfoot>tr.info>th{background-color:#d9edf7}.table-hover>tbody>tr>td.info:hover,.table-hover>tbody>tr>th.info:hover,.table-hover>tbody>tr.info:hover>td,.table-hover>tbody>tr.info:hover>th{background-color:#c4e3f3}.table>thead>tr>td.warning,.table>tbody>tr>td.warning,.table>tfoot>tr>td.warning,.table>thead>tr>th.warning,.table>tbody>tr>th.warning,.table>tfoot>tr>th.warning,.table>thead>tr.warning>td,.table>tbody>tr.warning>td,.table>tfoot>tr.warning>td,.table>thead>tr.warning>th,.table>tbody>tr.warning>th,.table>tfoot>tr.warning>th{background-color:#fcf8e3}.table-hover>tbody>tr>td.warning:hover,.table-hover>tbody>tr>th.warning:hover,.table-hover>tbody>tr.warning:hover>td,.table-hover>tbody>tr.warning:hover>th{background-color:#faf2cc}.table>thead>tr>td.danger,.table>tbody>tr>td.danger,.table>tfoot>tr>td.danger,.table>thead>tr>th.danger,.table>tbody>tr>th.danger,.table>tfoot>tr>th.danger,.table>thead>tr.danger>td,.table>tbody>tr.danger>td,.table>tfoot>tr.danger>td,.table>thead>tr.danger>th,.table>tbody>tr.danger>th,.table>tfoot>tr.danger>th{background-color:#f2dede}.table-hover>tbody>tr>td.danger:hover,.table-hover>tbody>tr>th.danger:hover,.table-hover>tbody>tr.danger:hover>td,.table-hover>tbody>tr.danger:hover>th{background-color:#ebcccc}@media (max-width:767px){.table-responsive{width:100%;margin-bottom:15px;overflow-y:hidden;overflow-x:scroll;-ms-overflow-style:-ms-autohiding-scrollbar;border:1px solid #ddd;-webkit-overflow-scrolling:touch}.table-responsive>.table{margin-bottom:0}.table-responsive>.table>thead>tr>th,.table-responsive>.table>tbody>tr>th,.table-responsive>.table>tfoot>tr>th,.table-responsive>.table>thead>tr>td,.table-responsive>.table>tbody>tr>td,.table-responsive>.table>tfoot>tr>td{white-space:nowrap}.table-responsive>.table-bordered{border:0}.table-responsive>.table-bordered>thead>tr>th:first-child,.table-responsive>.table-bordered>tbody>tr>th:first-child,.table-responsive>.table-bordered>tfoot>tr>th:first-child,.table-responsive>.table-bordered>thead>tr>td:first-child,.table-responsive>.table-bordered>tbody>tr>td:first-child,.table-responsive>.table-bordered>tfoot>tr>td:first-child{border-left:0}.table-responsive>.table-bordered>thead>tr>th:last-child,.table-responsive>.table-bordered>tbody>tr>th:last-child,.table-responsive>.table-bordered>tfoot>tr>th:last-child,.table-responsive>.table-bordered>thead>tr>td:last-child,.table-responsive>.table-bordered>tbody>tr>td:last-child,.table-responsive>.table-bordered>tfoot>tr>td:last-child{border-right:0}.table-responsive>.table-bordered>tbody>tr:last-child>th,.table-responsive>.table-bordered>tfoot>tr:last-child>th,.table-responsive>.table-bordered>tbody>tr:last-child>td,.table-responsive>.table-bordered>tfoot>tr:last-child>td{border-bottom:0}}fieldset{padding:0;margin:0;border:0;min-width:0}legend{display:block;width:100%;padding:0;margin-bottom:20px;font-size:21px;line-height:inherit;color:#333;border:0;border-bottom:1px solid #e5e5e5}label{display:inline-block;margin-bottom:5px;font-weight:700}input[type=search]{-webkit-box-sizing:border-box;-moz-box-sizing:border-box;box-sizing:border-box}input[type=radio],input[type=checkbox]{margin:4px 0 0;margin-top:1px \\9;line-height:normal}input[type=file]{display:block}input[type=range]{display:block;width:100%}select[multiple],select[size]{height:auto}input[type=file]:focus,input[type=radio]:focus,input[type=checkbox]:focus{outline:thin dotted;outline:5px auto -webkit-focus-ring-color;outline-offset:-2px}output{display:block;padding-top:7px;font-size:14px;line-height:1.42857143;color:#555}.form-control{display:block;width:100%;height:34px;padding:6px 12px;font-size:14px;line-height:1.42857143;color:#555;background-color:#fff;background-image:none;border:1px solid #ccc;border-radius:4px;-webkit-box-shadow:inset 0 1px 1px rgba(0,0,0,.075);box-shadow:inset 0 1px 1px rgba(0,0,0,.075);-webkit-transition:border-color ease-in-out .15s,box-shadow ease-in-out .15s;transition:border-color ease-in-out .15s,box-shadow ease-in-out .15s}.form-control:focus{border-color:#66afe9;outline:0;-webkit-box-shadow:inset 0 1px 1px rgba(0,0,0,.075),0 0 8px rgba(102,175,233,.6);box-shadow:inset 0 1px 1px rgba(0,0,0,.075),0 0 8px rgba(102,175,233,.6)}.form-control::-moz-placeholder{color:#999;opacity:1}.form-control:-ms-input-placeholder{color:#999}.form-control::-webkit-input-placeholder{color:#999}.form-control[disabled],.form-control[readonly],fieldset[disabled] .form-control{cursor:not-allowed;background-color:#eee;opacity:1}textarea.form-control{height:auto}input[type=search]{-webkit-appearance:none}input[type=date]{line-height:34px}.form-group{margin-bottom:15px}.radio,.checkbox{display:block;min-height:20px;margin-top:10px;margin-bottom:10px;padding-left:20px}.radio label,.checkbox label{display:inline;font-weight:400;cursor:pointer}.radio input[type=radio],.radio-inline input[type=radio],.checkbox input[type=checkbox],.checkbox-inline input[type=checkbox]{float:left;margin-left:-20px}.radio+.radio,.checkbox+.checkbox{margin-top:-5px}.radio-inline,.checkbox-inline{display:inline-block;padding-left:20px;margin-bottom:0;vertical-align:middle;font-weight:400;cursor:pointer}.radio-inline+.radio-inline,.checkbox-inline+.checkbox-inline{margin-top:0;margin-left:10px}input[type=radio][disabled],input[type=checkbox][disabled],.radio[disabled],.radio-inline[disabled],.checkbox[disabled],.checkbox-inline[disabled],fieldset[disabled] input[type=radio],fieldset[disabled] input[type=checkbox],fieldset[disabled] .radio,fieldset[disabled] .radio-inline,fieldset[disabled] .checkbox,fieldset[disabled] .checkbox-inline{cursor:not-allowed}.input-sm{height:30px;padding:5px 10px;font-size:12px;line-height:1.5;border-radius:3px}select.input-sm{height:30px;line-height:30px}textarea.input-sm,select[multiple].input-sm{height:auto}.input-lg{height:46px;padding:10px 16px;font-size:18px;line-height:1.33;border-radius:6px}select.input-lg{height:46px;line-height:46px}textarea.input-lg,select[multiple].input-lg{height:auto}.has-feedback{position:relative}.has-feedback .form-control{padding-right:42.5px}.has-feedback .form-control-feedback{position:absolute;top:25px;right:0;display:block;width:34px;height:34px;line-height:34px;text-align:center}.has-success .help-block,.has-success .control-label,.has-success .radio,.has-success .checkbox,.has-success .radio-inline,.has-success .checkbox-inline{color:#3c763d}.has-success .form-control{border-color:#3c763d;-webkit-box-shadow:inset 0 1px 1px rgba(0,0,0,.075);box-shadow:inset 0 1px 1px rgba(0,0,0,.075)}.has-success .form-control:focus{border-color:#2b542c;-webkit-box-shadow:inset 0 1px 1px rgba(0,0,0,.075),0 0 6px #67b168;box-shadow:inset 0 1px 1px rgba(0,0,0,.075),0 0 6px #67b168}.has-success .input-group-addon{color:#3c763d;border-color:#3c763d;background-color:#dff0d8}.has-success .form-control-feedback{color:#3c763d}.has-warning .help-block,.has-warning .control-label,.has-warning .radio,.has-warning .checkbox,.has-warning .radio-inline,.has-warning .checkbox-inline{color:#8a6d3b}.has-warning .form-control{border-color:#8a6d3b;-webkit-box-shadow:inset 0 1px 1px rgba(0,0,0,.075);box-shadow:inset 0 1px 1px rgba(0,0,0,.075)}.has-warning .form-control:focus{border-color:#66512c;-webkit-box-shadow:inset 0 1px 1px rgba(0,0,0,.075),0 0 6px #c0a16b;box-shadow:inset 0 1px 1px rgba(0,0,0,.075),0 0 6px #c0a16b}.has-warning .input-group-addon{color:#8a6d3b;border-color:#8a6d3b;background-color:#fcf8e3}.has-warning .form-control-feedback{color:#8a6d3b}.has-error .help-block,.has-error .control-label,.has-error .radio,.has-error .checkbox,.has-error .radio-inline,.has-error .checkbox-inline{color:#a94442}.has-error .form-control{border-color:#a94442;-webkit-box-shadow:inset 0 1px 1px rgba(0,0,0,.075);box-shadow:inset 0 1px 1px rgba(0,0,0,.075)}.has-error .form-control:focus{border-color:#843534;-webkit-box-shadow:inset 0 1px 1px rgba(0,0,0,.075),0 0 6px #ce8483;box-shadow:inset 0 1px 1px rgba(0,0,0,.075),0 0 6px #ce8483}.has-error .input-group-addon{color:#a94442;border-color:#a94442;background-color:#f2dede}.has-error .form-control-feedback{color:#a94442}.form-control-static{margin-bottom:0}.help-block{display:block;margin-top:5px;margin-bottom:10px;color:#737373}@media (min-width:768px){.form-inline .form-group{display:inline-block;margin-bottom:0;vertical-align:middle}.form-inline .form-control{display:inline-block;width:auto;vertical-align:middle}.form-inline .input-group>.form-control{width:100%}.form-inline .control-label{margin-bottom:0;vertical-align:middle}.form-inline .radio,.form-inline .checkbox{display:inline-block;margin-top:0;margin-bottom:0;padding-left:0;vertical-align:middle}.form-inline .radio input[type=radio],.form-inline .checkbox input[type=checkbox]{float:none;margin-left:0}.form-inline .has-feedback .form-control-feedback{top:0}}.form-horizontal .control-label,.form-horizontal .radio,.form-horizontal .checkbox,.form-horizontal .radio-inline,.form-horizontal .checkbox-inline{margin-top:0;margin-bottom:0;padding-top:7px}.form-horizontal .radio,.form-horizontal .checkbox{min-height:27px}.form-horizontal .form-group{margin-left:-15px;margin-right:-15px}.form-horizontal .form-control-static{padding-top:7px}@media (min-width:768px){.form-horizontal .control-label{text-align:right}}.form-horizontal .has-feedback .form-control-feedback{top:0;right:15px}.btn{display:inline-block;margin-bottom:0;font-weight:400;text-align:center;vertical-align:middle;cursor:pointer;background-image:none;border:1px solid transparent;white-space:nowrap;padding:6px 12px;font-size:14px;line-height:1.42857143;border-radius:4px;-webkit-user-select:none;-moz-user-select:none;-ms-user-select:none;user-select:none}.btn:focus,.btn:active:focus,.btn.active:focus{outline:thin dotted;outline:5px auto -webkit-focus-ring-color;outline-offset:-2px}.btn:hover,.btn:focus{color:#333;text-decoration:none}.btn:active,.btn.active{outline:0;background-image:none;-webkit-box-shadow:inset 0 3px 5px rgba(0,0,0,.125);box-shadow:inset 0 3px 5px rgba(0,0,0,.125)}.btn.disabled,.btn[disabled],fieldset[disabled] .btn{cursor:not-allowed;pointer-events:none;opacity:.65;filter:alpha(opacity=65);-webkit-box-shadow:none;box-shadow:none}.btn-default{color:#333;background-color:#fff;border-color:#ccc}.btn-default:hover,.btn-default:focus,.btn-default:active,.btn-default.active,.open .dropdown-toggle.btn-default{color:#333;background-color:#ebebeb;border-color:#adadad}.btn-default:active,.btn-default.active,.open .dropdown-toggle.btn-default{background-image:none}.btn-default.disabled,.btn-default[disabled],fieldset[disabled] .btn-default,.btn-default.disabled:hover,.btn-default[disabled]:hover,fieldset[disabled] .btn-default:hover,.btn-default.disabled:focus,.btn-default[disabled]:focus,fieldset[disabled] .btn-default:focus,.btn-default.disabled:active,.btn-default[disabled]:active,fieldset[disabled] .btn-default:active,.btn-default.disabled.active,.btn-default[disabled].active,fieldset[disabled] .btn-default.active{background-color:#fff;border-color:#ccc}.btn-default .badge{color:#fff;background-color:#333}.btn-primary{color:#fff;background-color:#428bca;border-color:#357ebd}.btn-primary:hover,.btn-primary:focus,.btn-primary:active,.btn-primary.active,.open .dropdown-toggle.btn-primary{color:#fff;background-color:#3276b1;border-color:#285e8e}.btn-primary:active,.btn-primary.active,.open .dropdown-toggle.btn-primary{background-image:none}.btn-primary.disabled,.btn-primary[disabled],fieldset[disabled] .btn-primary,.btn-primary.disabled:hover,.btn-primary[disabled]:hover,fieldset[disabled] .btn-primary:hover,.btn-primary.disabled:focus,.btn-primary[disabled]:focus,fieldset[disabled] .btn-primary:focus,.btn-primary.disabled:active,.btn-primary[disabled]:active,fieldset[disabled] .btn-primary:active,.btn-primary.disabled.active,.btn-primary[disabled].active,fieldset[disabled] .btn-primary.active{background-color:#428bca;border-color:#357ebd}.btn-primary .badge{color:#428bca;background-color:#fff}.btn-success{color:#fff;background-color:#5cb85c;border-color:#4cae4c}.btn-success:hover,.btn-success:focus,.btn-success:active,.btn-success.active,.open .dropdown-toggle.btn-success{color:#fff;background-color:#47a447;border-color:#398439}.btn-success:active,.btn-success.active,.open .dropdown-toggle.btn-success{background-image:none}.btn-success.disabled,.btn-success[disabled],fieldset[disabled] .btn-success,.btn-success.disabled:hover,.btn-success[disabled]:hover,fieldset[disabled] .btn-success:hover,.btn-success.disabled:focus,.btn-success[disabled]:focus,fieldset[disabled] .btn-success:focus,.btn-success.disabled:active,.btn-success[disabled]:active,fieldset[disabled] .btn-success:active,.btn-success.disabled.active,.btn-success[disabled].active,fieldset[disabled] .btn-success.active{background-color:#5cb85c;border-color:#4cae4c}.btn-success .badge{color:#5cb85c;background-color:#fff}.btn-info{color:#fff;background-color:#5bc0de;border-color:#46b8da}.btn-info:hover,.btn-info:focus,.btn-info:active,.btn-info.active,.open .dropdown-toggle.btn-info{color:#fff;background-color:#39b3d7;border-color:#269abc}.btn-info:active,.btn-info.active,.open .dropdown-toggle.btn-info{background-image:none}.btn-info.disabled,.btn-info[disabled],fieldset[disabled] .btn-info,.btn-info.disabled:hover,.btn-info[disabled]:hover,fieldset[disabled] .btn-info:hover,.btn-info.disabled:focus,.btn-info[disabled]:focus,fieldset[disabled] .btn-info:focus,.btn-info.disabled:active,.btn-info[disabled]:active,fieldset[disabled] .btn-info:active,.btn-info.disabled.active,.btn-info[disabled].active,fieldset[disabled] .btn-info.active{background-color:#5bc0de;border-color:#46b8da}.btn-info .badge{color:#5bc0de;background-color:#fff}.btn-warning{color:#fff;background-color:#f0ad4e;border-color:#eea236}.btn-warning:hover,.btn-warning:focus,.btn-warning:active,.btn-warning.active,.open .dropdown-toggle.btn-warning{color:#fff;background-color:#ed9c28;border-color:#d58512}.btn-warning:active,.btn-warning.active,.open .dropdown-toggle.btn-warning{background-image:none}.btn-warning.disabled,.btn-warning[disabled],fieldset[disabled] .btn-warning,.btn-warning.disabled:hover,.btn-warning[disabled]:hover,fieldset[disabled] .btn-warning:hover,.btn-warning.disabled:focus,.btn-warning[disabled]:focus,fieldset[disabled] .btn-warning:focus,.btn-warning.disabled:active,.btn-warning[disabled]:active,fieldset[disabled] .btn-warning:active,.btn-warning.disabled.active,.btn-warning[disabled].active,fieldset[disabled] .btn-warning.active{background-color:#f0ad4e;border-color:#eea236}.btn-warning .badge{color:#f0ad4e;background-color:#fff}.btn-danger{color:#fff;background-color:#d9534f;border-color:#d43f3a}.btn-danger:hover,.btn-danger:focus,.btn-danger:active,.btn-danger.active,.open .dropdown-toggle.btn-danger{color:#fff;background-color:#d2322d;border-color:#ac2925}.btn-danger:active,.btn-danger.active,.open .dropdown-toggle.btn-danger{background-image:none}.btn-danger.disabled,.btn-danger[disabled],fieldset[disabled] .btn-danger,.btn-danger.disabled:hover,.btn-danger[disabled]:hover,fieldset[disabled] .btn-danger:hover,.btn-danger.disabled:focus,.btn-danger[disabled]:focus,fieldset[disabled] .btn-danger:focus,.btn-danger.disabled:active,.btn-danger[disabled]:active,fieldset[disabled] .btn-danger:active,.btn-danger.disabled.active,.btn-danger[disabled].active,fieldset[disabled] .btn-danger.active{background-color:#d9534f;border-color:#d43f3a}.btn-danger .badge{color:#d9534f;background-color:#fff}.btn-link{color:#428bca;font-weight:400;cursor:pointer;border-radius:0}.btn-link,.btn-link:active,.btn-link[disabled],fieldset[disabled] .btn-link{background-color:transparent;-webkit-box-shadow:none;box-shadow:none}.btn-link,.btn-link:hover,.btn-link:focus,.btn-link:active{border-color:transparent}.btn-link:hover,.btn-link:focus{color:#2a6496;text-decoration:underline;background-color:transparent}.btn-link[disabled]:hover,fieldset[disabled] .btn-link:hover,.btn-link[disabled]:focus,fieldset[disabled] .btn-link:focus{color:#999;text-decoration:none}.btn-lg,.btn-group-lg>.btn{padding:10px 16px;font-size:18px;line-height:1.33;border-radius:6px}.btn-sm,.btn-group-sm>.btn{padding:5px 10px;font-size:12px;line-height:1.5;border-radius:3px}.btn-xs,.btn-group-xs>.btn{padding:1px 5px;font-size:12px;line-height:1.5;border-radius:3px}.btn-block{display:block;width:100%;padding-left:0;padding-right:0}.btn-block+.btn-block{margin-top:5px}input[type=submit].btn-block,input[type=reset].btn-block,input[type=button].btn-block{width:100%}.fade{opacity:0;-webkit-transition:opacity .15s linear;transition:opacity .15s linear}.fade.in{opacity:1}.collapse{display:none}.collapse.in{display:block}.collapsing{position:relative;height:0;overflow:hidden;-webkit-transition:height .35s ease;transition:height .35s ease}@font-face{font-family:'Glyphicons Halflings';src:url(../fonts/glyphicons-halflings-regular.eot);src:url(../fonts/glyphicons-halflings-regular.eot?#iefix) format('embedded-opentype'),url(../fonts/glyphicons-halflings-regular.woff) format('woff'),url(../fonts/glyphicons-halflings-regular.ttf) format('truetype'),url(../fonts/glyphicons-halflings-regular.svg#glyphicons_halflingsregular) format('svg')}.glyphicon{position:relative;top:1px;display:inline-block;font-family:'Glyphicons Halflings';font-style:normal;font-weight:400;line-height:1;-webkit-font-smoothing:antialiased;-moz-osx-font-smoothing:grayscale}.glyphicon-asterisk:before{content:\"\\2a\"}.glyphicon-plus:before{content:\"\\2b\"}.glyphicon-euro:before{content:\"\\20ac\"}.glyphicon-minus:before{content:\"\\2212\"}.glyphicon-cloud:before{content:\"\\2601\"}.glyphicon-envelope:before{content:\"\\2709\"}.glyphicon-pencil:before{content:\"\\270f\"}.glyphicon-glass:before{content:\"\\e001\"}.glyphicon-music:before{content:\"\\e002\"}.glyphicon-search:before{content:\"\\e003\"}.glyphicon-heart:before{content:\"\\e005\"}.glyphicon-star:before{content:\"\\e006\"}.glyphicon-star-empty:before{content:\"\\e007\"}.glyphicon-user:before{content:\"\\e008\"}.glyphicon-film:before{content:\"\\e009\"}.glyphicon-th-large:before{content:\"\\e010\"}.glyphicon-th:before{content:\"\\e011\"}.glyphicon-th-list:before{content:\"\\e012\"}.glyphicon-ok:before{content:\"\\e013\"}.glyphicon-remove:before{content:\"\\e014\"}.glyphicon-zoom-in:before{content:\"\\e015\"}.glyphicon-zoom-out:before{content:\"\\e016\"}.glyphicon-off:before{content:\"\\e017\"}.glyphicon-signal:before{content:\"\\e018\"}.glyphicon-cog:before{content:\"\\e019\"}.glyphicon-trash:before{content:\"\\e020\"}.glyphicon-home:before{content:\"\\e021\"}.glyphicon-file:before{content:\"\\e022\"}.glyphicon-time:before{content:\"\\e023\"}.glyphicon-road:before{content:\"\\e024\"}.glyphicon-download-alt:before{content:\"\\e025\"}.glyphicon-download:before{content:\"\\e026\"}.glyphicon-upload:before{content:\"\\e027\"}.glyphicon-inbox:before{content:\"\\e028\"}.glyphicon-play-circle:before{content:\"\\e029\"}.glyphicon-repeat:before{content:\"\\e030\"}.glyphicon-refresh:before{content:\"\\e031\"}.glyphicon-list-alt:before{content:\"\\e032\"}.glyphicon-lock:before{content:\"\\e033\"}.glyphicon-flag:before{content:\"\\e034\"}.glyphicon-headphones:before{content:\"\\e035\"}.glyphicon-volume-off:before{content:\"\\e036\"}.glyphicon-volume-down:before{content:\"\\e037\"}.glyphicon-volume-up:before{content:\"\\e038\"}.glyphicon-qrcode:before{content:\"\\e039\"}.glyphicon-barcode:before{content:\"\\e040\"}.glyphicon-tag:before{content:\"\\e041\"}.glyphicon-tags:before{content:\"\\e042\"}.glyphicon-book:before{content:\"\\e043\"}.glyphicon-bookmark:before{content:\"\\e044\"}.glyphicon-print:before{content:\"\\e045\"}.glyphicon-camera:before{content:\"\\e046\"}.glyphicon-font:before{content:\"\\e047\"}.glyphicon-bold:before{content:\"\\e048\"}.glyphicon-italic:before{content:\"\\e049\"}.glyphicon-text-height:before{content:\"\\e050\"}.glyphicon-text-width:before{content:\"\\e051\"}.glyphicon-align-left:before{content:\"\\e052\"}.glyphicon-align-center:before{content:\"\\e053\"}.glyphicon-align-right:before{content:\"\\e054\"}.glyphicon-align-justify:before{content:\"\\e055\"}.glyphicon-list:before{content:\"\\e056\"}.glyphicon-indent-left:before{content:\"\\e057\"}.glyphicon-indent-right:before{content:\"\\e058\"}.glyphicon-facetime-video:before{content:\"\\e059\"}.glyphicon-picture:before{content:\"\\e060\"}.glyphicon-map-marker:before{content:\"\\e062\"}.glyphicon-adjust:before{content:\"\\e063\"}.glyphicon-tint:before{content:\"\\e064\"}.glyphicon-edit:before{content:\"\\e065\"}.glyphicon-share:before{content:\"\\e066\"}.glyphicon-check:before{content:\"\\e067\"}.glyphicon-move:before{content:\"\\e068\"}.glyphicon-step-backward:before{content:\"\\e069\"}.glyphicon-fast-backward:before{content:\"\\e070\"}.glyphicon-backward:before{content:\"\\e071\"}.glyphicon-play:before{content:\"\\e072\"}.glyphicon-pause:before{content:\"\\e073\"}.glyphicon-stop:before{content:\"\\e074\"}.glyphicon-forward:before{content:\"\\e075\"}.glyphicon-fast-forward:before{content:\"\\e076\"}.glyphicon-step-forward:before{content:\"\\e077\"}.glyphicon-eject:before{content:\"\\e078\"}.glyphicon-chevron-left:before{content:\"\\e079\"}.glyphicon-chevron-right:before{content:\"\\e080\"}.glyphicon-plus-sign:before{content:\"\\e081\"}.glyphicon-minus-sign:before{content:\"\\e082\"}.glyphicon-remove-sign:before{content:\"\\e083\"}.glyphicon-ok-sign:before{content:\"\\e084\"}.glyphicon-question-sign:before{content:\"\\e085\"}.glyphicon-info-sign:before{content:\"\\e086\"}.glyphicon-screenshot:before{content:\"\\e087\"}.glyphicon-remove-circle:before{content:\"\\e088\"}.glyphicon-ok-circle:before{content:\"\\e089\"}.glyphicon-ban-circle:before{content:\"\\e090\"}.glyphicon-arrow-left:before{content:\"\\e091\"}.glyphicon-arrow-right:before{content:\"\\e092\"}.glyphicon-arrow-up:before{content:\"\\e093\"}.glyphicon-arrow-down:before{content:\"\\e094\"}.glyphicon-share-alt:before{content:\"\\e095\"}.glyphicon-resize-full:before{content:\"\\e096\"}.glyphicon-resize-small:before{content:\"\\e097\"}.glyphicon-exclamation-sign:before{content:\"\\e101\"}.glyphicon-gift:before{content:\"\\e102\"}.glyphicon-leaf:before{content:\"\\e103\"}.glyphicon-fire:before{content:\"\\e104\"}.glyphicon-eye-open:before{content:\"\\e105\"}.glyphicon-eye-close:before{content:\"\\e106\"}.glyphicon-warning-sign:before{content:\"\\e107\"}.glyphicon-plane:before{content:\"\\e108\"}.glyphicon-calendar:before{content:\"\\e109\"}.glyphicon-random:before{content:\"\\e110\"}.glyphicon-comment:before{content:\"\\e111\"}.glyphicon-magnet:before{content:\"\\e112\"}.glyphicon-chevron-up:before{content:\"\\e113\"}.glyphicon-chevron-down:before{content:\"\\e114\"}.glyphicon-retweet:before{content:\"\\e115\"}.glyphicon-shopping-cart:before{content:\"\\e116\"}.glyphicon-folder-close:before{content:\"\\e117\"}.glyphicon-folder-open:before{content:\"\\e118\"}.glyphicon-resize-vertical:before{content:\"\\e119\"}.glyphicon-resize-horizontal:before{content:\"\\e120\"}.glyphicon-hdd:before{content:\"\\e121\"}.glyphicon-bullhorn:before{content:\"\\e122\"}.glyphicon-bell:before{content:\"\\e123\"}.glyphicon-certificate:before{content:\"\\e124\"}.glyphicon-thumbs-up:before{content:\"\\e125\"}.glyphicon-thumbs-down:before{content:\"\\e126\"}.glyphicon-hand-right:before{content:\"\\e127\"}.glyphicon-hand-left:before{content:\"\\e128\"}.glyphicon-hand-up:before{content:\"\\e129\"}.glyphicon-hand-down:before{content:\"\\e130\"}.glyphicon-circle-arrow-right:before{content:\"\\e131\"}.glyphicon-circle-arrow-left:before{content:\"\\e132\"}.glyphicon-circle-arrow-up:before{content:\"\\e133\"}.glyphicon-circle-arrow-down:before{content:\"\\e134\"}.glyphicon-globe:before{content:\"\\e135\"}.glyphicon-wrench:before{content:\"\\e136\"}.glyphicon-tasks:before{content:\"\\e137\"}.glyphicon-filter:before{content:\"\\e138\"}.glyphicon-briefcase:before{content:\"\\e139\"}.glyphicon-fullscreen:before{content:\"\\e140\"}.glyphicon-dashboard:before{content:\"\\e141\"}.glyphicon-paperclip:before{content:\"\\e142\"}.glyphicon-heart-empty:before{content:\"\\e143\"}.glyphicon-link:before{content:\"\\e144\"}.glyphicon-phone:before{content:\"\\e145\"}.glyphicon-pushpin:before{content:\"\\e146\"}.glyphicon-usd:before{content:\"\\e148\"}.glyphicon-gbp:before{content:\"\\e149\"}.glyphicon-sort:before{content:\"\\e150\"}.glyphicon-sort-by-alphabet:before{content:\"\\e151\"}.glyphicon-sort-by-alphabet-alt:before{content:\"\\e152\"}.glyphicon-sort-by-order:before{content:\"\\e153\"}.glyphicon-sort-by-order-alt:before{content:\"\\e154\"}.glyphicon-sort-by-attributes:before{content:\"\\e155\"}.glyphicon-sort-by-attributes-alt:before{content:\"\\e156\"}.glyphicon-unchecked:before{content:\"\\e157\"}.glyphicon-expand:before{content:\"\\e158\"}.glyphicon-collapse-down:before{content:\"\\e159\"}.glyphicon-collapse-up:before{content:\"\\e160\"}.glyphicon-log-in:before{content:\"\\e161\"}.glyphicon-flash:before{content:\"\\e162\"}.glyphicon-log-out:before{content:\"\\e163\"}.glyphicon-new-window:before{content:\"\\e164\"}.glyphicon-record:before{content:\"\\e165\"}.glyphicon-save:before{content:\"\\e166\"}.glyphicon-open:before{content:\"\\e167\"}.glyphicon-saved:before{content:\"\\e168\"}.glyphicon-import:before{content:\"\\e169\"}.glyphicon-export:before{content:\"\\e170\"}.glyphicon-send:before{content:\"\\e171\"}.glyphicon-floppy-disk:before{content:\"\\e172\"}.glyphicon-floppy-saved:before{content:\"\\e173\"}.glyphicon-floppy-remove:before{content:\"\\e174\"}.glyphicon-floppy-save:before{content:\"\\e175\"}.glyphicon-floppy-open:before{content:\"\\e176\"}.glyphicon-credit-card:before{content:\"\\e177\"}.glyphicon-transfer:before{content:\"\\e178\"}.glyphicon-cutlery:before{content:\"\\e179\"}.glyphicon-header:before{content:\"\\e180\"}.glyphicon-compressed:before{content:\"\\e181\"}.glyphicon-earphone:before{content:\"\\e182\"}.glyphicon-phone-alt:before{content:\"\\e183\"}.glyphicon-tower:before{content:\"\\e184\"}.glyphicon-stats:before{content:\"\\e185\"}.glyphicon-sd-video:before{content:\"\\e186\"}.glyphicon-hd-video:before{content:\"\\e187\"}.glyphicon-subtitles:before{content:\"\\e188\"}.glyphicon-sound-stereo:before{content:\"\\e189\"}.glyphicon-sound-dolby:before{content:\"\\e190\"}.glyphicon-sound-5-1:before{content:\"\\e191\"}.glyphicon-sound-6-1:before{content:\"\\e192\"}.glyphicon-sound-7-1:before{content:\"\\e193\"}.glyphicon-copyright-mark:before{content:\"\\e194\"}.glyphicon-registration-mark:before{content:\"\\e195\"}.glyphicon-cloud-download:before{content:\"\\e197\"}.glyphicon-cloud-upload:before{content:\"\\e198\"}.glyphicon-tree-conifer:before{content:\"\\e199\"}.glyphicon-tree-deciduous:before{content:\"\\e200\"}.caret{display:inline-block;width:0;height:0;margin-left:2px;vertical-align:middle;border-top:4px solid;border-right:4px solid transparent;border-left:4px solid transparent}.dropdown{position:relative}.dropdown-toggle:focus{outline:0}.dropdown-menu{position:absolute;top:100%;left:0;z-index:1000;display:none;float:left;min-width:160px;padding:5px 0;margin:2px 0 0;list-style:none;font-size:14px;background-color:#fff;border:1px solid #ccc;border:1px solid rgba(0,0,0,.15);border-radius:4px;-webkit-box-shadow:0 6px 12px rgba(0,0,0,.175);box-shadow:0 6px 12px rgba(0,0,0,.175);background-clip:padding-box}.dropdown-menu.pull-right{right:0;left:auto}.dropdown-menu .divider{height:1px;margin:9px 0;overflow:hidden;background-color:#e5e5e5}.dropdown-menu>li>a{display:block;padding:3px 20px;clear:both;font-weight:400;line-height:1.42857143;color:#333;white-space:nowrap}.dropdown-menu>li>a:hover,.dropdown-menu>li>a:focus{text-decoration:none;color:#262626;background-color:#f5f5f5}.dropdown-menu>.active>a,.dropdown-menu>.active>a:hover,.dropdown-menu>.active>a:focus{color:#fff;text-decoration:none;outline:0;background-color:#428bca}.dropdown-menu>.disabled>a,.dropdown-menu>.disabled>a:hover,.dropdown-menu>.disabled>a:focus{color:#999}.dropdown-menu>.disabled>a:hover,.dropdown-menu>.disabled>a:focus{text-decoration:none;background-color:transparent;background-image:none;filter:progid:DXImageTransform.Microsoft.gradient(enabled=false);cursor:not-allowed}.open>.dropdown-menu{display:block}.open>a{outline:0}.dropdown-menu-right{left:auto;right:0}.dropdown-menu-left{left:0;right:auto}.dropdown-header{display:block;padding:3px 20px;font-size:12px;line-height:1.42857143;color:#999}.dropdown-backdrop{position:fixed;left:0;right:0;bottom:0;top:0;z-index:990}.pull-right>.dropdown-menu{right:0;left:auto}.dropup .caret,.navbar-fixed-bottom .dropdown .caret{border-top:0;border-bottom:4px solid;content:\"\"}.dropup .dropdown-menu,.navbar-fixed-bottom .dropdown .dropdown-menu{top:auto;bottom:100%;margin-bottom:1px}@media (min-width:768px){.navbar-right .dropdown-menu{left:auto;right:0}.navbar-right .dropdown-menu-left{left:0;right:auto}}.btn-group,.btn-group-vertical{position:relative;display:inline-block;vertical-align:middle}.btn-group>.btn,.btn-group-vertical>.btn{position:relative;float:left}.btn-group>.btn:hover,.btn-group-vertical>.btn:hover,.btn-group>.btn:focus,.btn-group-vertical>.btn:focus,.btn-group>.btn:active,.btn-group-vertical>.btn:active,.btn-group>.btn.active,.btn-group-vertical>.btn.active{z-index:2}.btn-group>.btn:focus,.btn-group-vertical>.btn:focus{outline:0}.btn-group .btn+.btn,.btn-group .btn+.btn-group,.btn-group .btn-group+.btn,.btn-group .btn-group+.btn-group{margin-left:-1px}.btn-toolbar{margin-left:-5px}.btn-toolbar .btn-group,.btn-toolbar .input-group{float:left}.btn-toolbar>.btn,.btn-toolbar>.btn-group,.btn-toolbar>.input-group{margin-left:5px}.btn-group>.btn:not(:first-child):not(:last-child):not(.dropdown-toggle){border-radius:0}.btn-group>.btn:first-child{margin-left:0}.btn-group>.btn:first-child:not(:last-child):not(.dropdown-toggle){border-bottom-right-radius:0;border-top-right-radius:0}.btn-group>.btn:last-child:not(:first-child),.btn-group>.dropdown-toggle:not(:first-child){border-bottom-left-radius:0;border-top-left-radius:0}.btn-group>.btn-group{float:left}.btn-group>.btn-group:not(:first-child):not(:last-child)>.btn{border-radius:0}.btn-group>.btn-group:first-child>.btn:last-child,.btn-group>.btn-group:first-child>.dropdown-toggle{border-bottom-right-radius:0;border-top-right-radius:0}.btn-group>.btn-group:last-child>.btn:first-child{border-bottom-left-radius:0;border-top-left-radius:0}.btn-group .dropdown-toggle:active,.btn-group.open .dropdown-toggle{outline:0}.btn-group>.btn+.dropdown-toggle{padding-left:8px;padding-right:8px}.btn-group>.btn-lg+.dropdown-toggle{padding-left:12px;padding-right:12px}.btn-group.open .dropdown-toggle{-webkit-box-shadow:inset 0 3px 5px rgba(0,0,0,.125);box-shadow:inset 0 3px 5px rgba(0,0,0,.125)}.btn-group.open .dropdown-toggle.btn-link{-webkit-box-shadow:none;box-shadow:none}.btn .caret{margin-left:0}.btn-lg .caret{border-width:5px 5px 0;border-bottom-width:0}.dropup .btn-lg .caret{border-width:0 5px 5px}.btn-group-vertical>.btn,.btn-group-vertical>.btn-group,.btn-group-vertical>.btn-group>.btn{display:block;float:none;width:100%;max-width:100%}.btn-group-vertical>.btn-group>.btn{float:none}.btn-group-vertical>.btn+.btn,.btn-group-vertical>.btn+.btn-group,.btn-group-vertical>.btn-group+.btn,.btn-group-vertical>.btn-group+.btn-group{margin-top:-1px;margin-left:0}.btn-group-vertical>.btn:not(:first-child):not(:last-child){border-radius:0}.btn-group-vertical>.btn:first-child:not(:last-child){border-top-right-radius:4px;border-bottom-right-radius:0;border-bottom-left-radius:0}.btn-group-vertical>.btn:last-child:not(:first-child){border-bottom-left-radius:4px;border-top-right-radius:0;border-top-left-radius:0}.btn-group-vertical>.btn-group:not(:first-child):not(:last-child)>.btn{border-radius:0}.btn-group-vertical>.btn-group:first-child:not(:last-child)>.btn:last-child,.btn-group-vertical>.btn-group:first-child:not(:last-child)>.dropdown-toggle{border-bottom-right-radius:0;border-bottom-left-radius:0}.btn-group-vertical>.btn-group:last-child:not(:first-child)>.btn:first-child{border-top-right-radius:0;border-top-left-radius:0}.btn-group-justified{display:table;width:100%;table-layout:fixed;border-collapse:separate}.btn-group-justified>.btn,.btn-group-justified>.btn-group{float:none;display:table-cell;width:1%}.btn-group-justified>.btn-group .btn{width:100%}[data-toggle=buttons]>.btn>input[type=radio],[data-toggle=buttons]>.btn>input[type=checkbox]{display:none}.input-group{position:relative;display:table;border-collapse:separate}.input-group[class*=col-]{float:none;padding-left:0;padding-right:0}.input-group .form-control{position:relative;z-index:2;float:left;width:100%;margin-bottom:0}.input-group-lg>.form-control,.input-group-lg>.input-group-addon,.input-group-lg>.input-group-btn>.btn{height:46px;padding:10px 16px;font-size:18px;line-height:1.33;border-radius:6px}select.input-group-lg>.form-control,select.input-group-lg>.input-group-addon,select.input-group-lg>.input-group-btn>.btn{height:46px;line-height:46px}textarea.input-group-lg>.form-control,textarea.input-group-lg>.input-group-addon,textarea.input-group-lg>.input-group-btn>.btn,select[multiple].input-group-lg>.form-control,select[multiple].input-group-lg>.input-group-addon,select[multiple].input-group-lg>.input-group-btn>.btn{height:auto}.input-group-sm>.form-control,.input-group-sm>.input-group-addon,.input-group-sm>.input-group-btn>.btn{height:30px;padding:5px 10px;font-size:12px;line-height:1.5;border-radius:3px}select.input-group-sm>.form-control,select.input-group-sm>.input-group-addon,select.input-group-sm>.input-group-btn>.btn{height:30px;line-height:30px}textarea.input-group-sm>.form-control,textarea.input-group-sm>.input-group-addon,textarea.input-group-sm>.input-group-btn>.btn,select[multiple].input-group-sm>.form-control,select[multiple].input-group-sm>.input-group-addon,select[multiple].input-group-sm>.input-group-btn>.btn{height:auto}.input-group-addon,.input-group-btn,.input-group .form-control{display:table-cell}.input-group-addon:not(:first-child):not(:last-child),.input-group-btn:not(:first-child):not(:last-child),.input-group .form-control:not(:first-child):not(:last-child){border-radius:0}.input-group-addon,.input-group-btn{width:1%;white-space:nowrap;vertical-align:middle}.input-group-addon{padding:6px 12px;font-size:14px;font-weight:400;line-height:1;color:#555;text-align:center;background-color:#eee;border:1px solid #ccc;border-radius:4px}.input-group-addon.input-sm{padding:5px 10px;font-size:12px;border-radius:3px}.input-group-addon.input-lg{padding:10px 16px;font-size:18px;border-radius:6px}.input-group-addon input[type=radio],.input-group-addon input[type=checkbox]{margin-top:0}.input-group .form-control:first-child,.input-group-addon:first-child,.input-group-btn:first-child>.btn,.input-group-btn:first-child>.btn-group>.btn,.input-group-btn:first-child>.dropdown-toggle,.input-group-btn:last-child>.btn:not(:last-child):not(.dropdown-toggle),.input-group-btn:last-child>.btn-group:not(:last-child)>.btn{border-bottom-right-radius:0;border-top-right-radius:0}.input-group-addon:first-child{border-right:0}.input-group .form-control:last-child,.input-group-addon:last-child,.input-group-btn:last-child>.btn,.input-group-btn:last-child>.btn-group>.btn,.input-group-btn:last-child>.dropdown-toggle,.input-group-btn:first-child>.btn:not(:first-child),.input-group-btn:first-child>.btn-group:not(:first-child)>.btn{border-bottom-left-radius:0;border-top-left-radius:0}.input-group-addon:last-child{border-left:0}.input-group-btn{position:relative;font-size:0;white-space:nowrap}.input-group-btn>.btn{position:relative}.input-group-btn>.btn+.btn{margin-left:-1px}.input-group-btn>.btn:hover,.input-group-btn>.btn:focus,.input-group-btn>.btn:active{z-index:2}.input-group-btn:first-child>.btn,.input-group-btn:first-child>.btn-group{margin-right:-1px}.input-group-btn:last-child>.btn,.input-group-btn:last-child>.btn-group{margin-left:-1px}.nav{margin-bottom:0;padding-left:0;list-style:none}.nav>li{position:relative;display:block}.nav>li>a{position:relative;display:block;padding:10px 15px}.nav>li>a:hover,.nav>li>a:focus{text-decoration:none;background-color:#eee}.nav>li.disabled>a{color:#999}.nav>li.disabled>a:hover,.nav>li.disabled>a:focus{color:#999;text-decoration:none;background-color:transparent;cursor:not-allowed}.nav .open>a,.nav .open>a:hover,.nav .open>a:focus{background-color:#eee;border-color:#428bca}.nav .nav-divider{height:1px;margin:9px 0;overflow:hidden;background-color:#e5e5e5}.nav>li>a>img{max-width:none}.nav-tabs{border-bottom:1px solid #ddd}.nav-tabs>li{float:left;margin-bottom:-1px}.nav-tabs>li>a{margin-right:2px;line-height:1.42857143;border:1px solid transparent;border-radius:4px 4px 0 0}.nav-tabs>li>a:hover{border-color:#eee #eee #ddd}.nav-tabs>li.active>a,.nav-tabs>li.active>a:hover,.nav-tabs>li.active>a:focus{color:#555;background-color:#fff;border:1px solid #ddd;border-bottom-color:transparent;cursor:default}.nav-tabs.nav-justified{width:100%;border-bottom:0}.nav-tabs.nav-justified>li{float:none}.nav-tabs.nav-justified>li>a{text-align:center;margin-bottom:5px}.nav-tabs.nav-justified>.dropdown .dropdown-menu{top:auto;left:auto}@media (min-width:768px){.nav-tabs.nav-justified>li{display:table-cell;width:1%}.nav-tabs.nav-justified>li>a{margin-bottom:0}}.nav-tabs.nav-justified>li>a{margin-right:0;border-radius:4px}.nav-tabs.nav-justified>.active>a,.nav-tabs.nav-justified>.active>a:hover,.nav-tabs.nav-justified>.active>a:focus{border:1px solid #ddd}@media (min-width:768px){.nav-tabs.nav-justified>li>a{border-bottom:1px solid #ddd;border-radius:4px 4px 0 0}.nav-tabs.nav-justified>.active>a,.nav-tabs.nav-justified>.active>a:hover,.nav-tabs.nav-justified>.active>a:focus{border-bottom-color:#fff}}.nav-pills>li{float:left}.nav-pills>li>a{border-radius:4px}.nav-pills>li+li{margin-left:2px}.nav-pills>li.active>a,.nav-pills>li.active>a:hover,.nav-pills>li.active>a:focus{color:#fff;background-color:#428bca}.nav-stacked>li{float:none}.nav-stacked>li+li{margin-top:2px;margin-left:0}.nav-justified{width:100%}.nav-justified>li{float:none}.nav-justified>li>a{text-align:center;margin-bottom:5px}.nav-justified>.dropdown .dropdown-menu{top:auto;left:auto}@media (min-width:768px){.nav-justified>li{display:table-cell;width:1%}.nav-justified>li>a{margin-bottom:0}}.nav-tabs-justified{border-bottom:0}.nav-tabs-justified>li>a{margin-right:0;border-radius:4px}.nav-tabs-justified>.active>a,.nav-tabs-justified>.active>a:hover,.nav-tabs-justified>.active>a:focus{border:1px solid #ddd}@media (min-width:768px){.nav-tabs-justified>li>a{border-bottom:1px solid #ddd;border-radius:4px 4px 0 0}.nav-tabs-justified>.active>a,.nav-tabs-justified>.active>a:hover,.nav-tabs-justified>.active>a:focus{border-bottom-color:#fff}}.tab-content>.tab-pane{display:none}.tab-content>.active{display:block}.nav-tabs .dropdown-menu{margin-top:-1px;border-top-right-radius:0;border-top-left-radius:0}.navbar{position:relative;min-height:50px;margin-bottom:20px;border:1px solid transparent}@media (min-width:768px){.navbar{border-radius:4px}}@media (min-width:768px){.navbar-header{float:left}}.navbar-collapse{max-height:340px;overflow-x:visible;padding-right:15px;padding-left:15px;border-top:1px solid transparent;box-shadow:inset 0 1px 0 rgba(255,255,255,.1);-webkit-overflow-scrolling:touch}.navbar-collapse.in{overflow-y:auto}@media (min-width:768px){.navbar-collapse{width:auto;border-top:0;box-shadow:none}.navbar-collapse.collapse{display:block!important;height:auto!important;padding-bottom:0;overflow:visible!important}.navbar-collapse.in{overflow-y:visible}.navbar-fixed-top .navbar-collapse,.navbar-static-top .navbar-collapse,.navbar-fixed-bottom .navbar-collapse{padding-left:0;padding-right:0}}.container>.navbar-header,.container-fluid>.navbar-header,.container>.navbar-collapse,.container-fluid>.navbar-collapse{margin-right:-15px;margin-left:-15px}@media (min-width:768px){.container>.navbar-header,.container-fluid>.navbar-header,.container>.navbar-collapse,.container-fluid>.navbar-collapse{margin-right:0;margin-left:0}}.navbar-static-top{z-index:1000;border-width:0 0 1px}@media (min-width:768px){.navbar-static-top{border-radius:0}}.navbar-fixed-top,.navbar-fixed-bottom{position:fixed;right:0;left:0;z-index:1030}@media (min-width:768px){.navbar-fixed-top,.navbar-fixed-bottom{border-radius:0}}.navbar-fixed-top{top:0;border-width:0 0 1px}.navbar-fixed-bottom{bottom:0;margin-bottom:0;border-width:1px 0 0}.navbar-brand{float:left;padding:15px;font-size:18px;line-height:20px;height:50px}.navbar-brand:hover,.navbar-brand:focus{text-decoration:none}@media (min-width:768px){.navbar>.container .navbar-brand,.navbar>.container-fluid .navbar-brand{margin-left:-15px}}.navbar-toggle{position:relative;float:right;margin-right:15px;padding:9px 10px;margin-top:8px;margin-bottom:8px;background-color:transparent;background-image:none;border:1px solid transparent;border-radius:4px}.navbar-toggle:focus{outline:0}.navbar-toggle .icon-bar{display:block;width:22px;height:2px;border-radius:1px}.navbar-toggle .icon-bar+.icon-bar{margin-top:4px}@media (min-width:768px){.navbar-toggle{display:none}}.navbar-nav{margin:7.5px -15px}.navbar-nav>li>a{padding-top:10px;padding-bottom:10px;line-height:20px}@media (max-width:767px){.navbar-nav .open .dropdown-menu{position:static;float:none;width:auto;margin-top:0;background-color:transparent;border:0;box-shadow:none}.navbar-nav .open .dropdown-menu>li>a,.navbar-nav .open .dropdown-menu .dropdown-header{padding:5px 15px 5px 25px}.navbar-nav .open .dropdown-menu>li>a{line-height:20px}.navbar-nav .open .dropdown-menu>li>a:hover,.navbar-nav .open .dropdown-menu>li>a:focus{background-image:none}}@media (min-width:768px){.navbar-nav{float:left;margin:0}.navbar-nav>li{float:left}.navbar-nav>li>a{padding-top:15px;padding-bottom:15px}.navbar-nav.navbar-right:last-child{margin-right:-15px}}@media (min-width:768px){.navbar-left{float:left!important}.navbar-right{float:right!important}}.navbar-form{margin-left:-15px;margin-right:-15px;padding:10px 15px;border-top:1px solid transparent;border-bottom:1px solid transparent;-webkit-box-shadow:inset 0 1px 0 rgba(255,255,255,.1),0 1px 0 rgba(255,255,255,.1);box-shadow:inset 0 1px 0 rgba(255,255,255,.1),0 1px 0 rgba(255,255,255,.1);margin-top:8px;margin-bottom:8px}@media (min-width:768px){.navbar-form .form-group{display:inline-block;margin-bottom:0;vertical-align:middle}.navbar-form .form-control{display:inline-block;width:auto;vertical-align:middle}.navbar-form .input-group>.form-control{width:100%}.navbar-form .control-label{margin-bottom:0;vertical-align:middle}.navbar-form .radio,.navbar-form .checkbox{display:inline-block;margin-top:0;margin-bottom:0;padding-left:0;vertical-align:middle}.navbar-form .radio input[type=radio],.navbar-form .checkbox input[type=checkbox]{float:none;margin-left:0}.navbar-form .has-feedback .form-control-feedback{top:0}}@media (max-width:767px){.navbar-form .form-group{margin-bottom:5px}}@media (min-width:768px){.navbar-form{width:auto;border:0;margin-left:0;margin-right:0;padding-top:0;padding-bottom:0;-webkit-box-shadow:none;box-shadow:none}.navbar-form.navbar-right:last-child{margin-right:-15px}}.navbar-nav>li>.dropdown-menu{margin-top:0;border-top-right-radius:0;border-top-left-radius:0}.navbar-fixed-bottom .navbar-nav>li>.dropdown-menu{border-bottom-right-radius:0;border-bottom-left-radius:0}.navbar-btn{margin-top:8px;margin-bottom:8px}.navbar-btn.btn-sm{margin-top:10px;margin-bottom:10px}.navbar-btn.btn-xs{margin-top:14px;margin-bottom:14px}.navbar-text{margin-top:15px;margin-bottom:15px}@media (min-width:768px){.navbar-text{float:left;margin-left:15px;margin-right:15px}.navbar-text.navbar-right:last-child{margin-right:0}}.navbar-default{background-color:#f8f8f8;border-color:#e7e7e7}.navbar-default .navbar-brand{color:#777}.navbar-default .navbar-brand:hover,.navbar-default .navbar-brand:focus{color:#5e5e5e;background-color:transparent}.navbar-default .navbar-text{color:#777}.navbar-default .navbar-nav>li>a{color:#777}.navbar-default .navbar-nav>li>a:hover,.navbar-default .navbar-nav>li>a:focus{color:#333;background-color:transparent}.navbar-default .navbar-nav>.active>a,.navbar-default .navbar-nav>.active>a:hover,.navbar-default .navbar-nav>.active>a:focus{color:#555;background-color:#e7e7e7}.navbar-default .navbar-nav>.disabled>a,.navbar-default .navbar-nav>.disabled>a:hover,.navbar-default .navbar-nav>.disabled>a:focus{color:#ccc;background-color:transparent}.navbar-default .navbar-toggle{border-color:#ddd}.navbar-default .navbar-toggle:hover,.navbar-default .navbar-toggle:focus{background-color:#ddd}.navbar-default .navbar-toggle .icon-bar{background-color:#888}.navbar-default .navbar-collapse,.navbar-default .navbar-form{border-color:#e7e7e7}.navbar-default .navbar-nav>.open>a,.navbar-default .navbar-nav>.open>a:hover,.navbar-default .navbar-nav>.open>a:focus{background-color:#e7e7e7;color:#555}@media (max-width:767px){.navbar-default .navbar-nav .open .dropdown-menu>li>a{color:#777}.navbar-default .navbar-nav .open .dropdown-menu>li>a:hover,.navbar-default .navbar-nav .open .dropdown-menu>li>a:focus{color:#333;background-color:transparent}.navbar-default .navbar-nav .open .dropdown-menu>.active>a,.navbar-default .navbar-nav .open .dropdown-menu>.active>a:hover,.navbar-default .navbar-nav .open .dropdown-menu>.active>a:focus{color:#555;background-color:#e7e7e7}.navbar-default .navbar-nav .open .dropdown-menu>.disabled>a,.navbar-default .navbar-nav .open .dropdown-menu>.disabled>a:hover,.navbar-default .navbar-nav .open .dropdown-menu>.disabled>a:focus{color:#ccc;background-color:transparent}}.navbar-default .navbar-link{color:#777}.navbar-default .navbar-link:hover{color:#333}.navbar-inverse{background-color:#222;border-color:#080808}.navbar-inverse .navbar-brand{color:#999}.navbar-inverse .navbar-brand:hover,.navbar-inverse .navbar-brand:focus{color:#fff;background-color:transparent}.navbar-inverse .navbar-text{color:#999}.navbar-inverse .navbar-nav>li>a{color:#999}.navbar-inverse .navbar-nav>li>a:hover,.navbar-inverse .navbar-nav>li>a:focus{color:#fff;background-color:transparent}.navbar-inverse .navbar-nav>.active>a,.navbar-inverse .navbar-nav>.active>a:hover,.navbar-inverse .navbar-nav>.active>a:focus{color:#fff;background-color:#080808}.navbar-inverse .navbar-nav>.disabled>a,.navbar-inverse .navbar-nav>.disabled>a:hover,.navbar-inverse .navbar-nav>.disabled>a:focus{color:#444;background-color:transparent}.navbar-inverse .navbar-toggle{border-color:#333}.navbar-inverse .navbar-toggle:hover,.navbar-inverse .navbar-toggle:focus{background-color:#333}.navbar-inverse .navbar-toggle .icon-bar{background-color:#fff}.navbar-inverse .navbar-collapse,.navbar-inverse .navbar-form{border-color:#101010}.navbar-inverse .navbar-nav>.open>a,.navbar-inverse .navbar-nav>.open>a:hover,.navbar-inverse .navbar-nav>.open>a:focus{background-color:#080808;color:#fff}@media (max-width:767px){.navbar-inverse .navbar-nav .open .dropdown-menu>.dropdown-header{border-color:#080808}.navbar-inverse .navbar-nav .open .dropdown-menu .divider{background-color:#080808}.navbar-inverse .navbar-nav .open .dropdown-menu>li>a{color:#999}.navbar-inverse .navbar-nav .open .dropdown-menu>li>a:hover,.navbar-inverse .navbar-nav .open .dropdown-menu>li>a:focus{color:#fff;background-color:transparent}.navbar-inverse .navbar-nav .open .dropdown-menu>.active>a,.navbar-inverse .navbar-nav .open .dropdown-menu>.active>a:hover,.navbar-inverse .navbar-nav .open .dropdown-menu>.active>a:focus{color:#fff;background-color:#080808}.navbar-inverse .navbar-nav .open .dropdown-menu>.disabled>a,.navbar-inverse .navbar-nav .open .dropdown-menu>.disabled>a:hover,.navbar-inverse .navbar-nav .open .dropdown-menu>.disabled>a:focus{color:#444;background-color:transparent}}.navbar-inverse .navbar-link{color:#999}.navbar-inverse .navbar-link:hover{color:#fff}.breadcrumb{padding:8px 15px;margin-bottom:20px;list-style:none;background-color:#f5f5f5;border-radius:4px}.breadcrumb>li{display:inline-block}.breadcrumb>li+li:before{content:\"/\\00a0\";padding:0 5px;color:#ccc}.breadcrumb>.active{color:#999}.pagination{display:inline-block;padding-left:0;margin:20px 0;border-radius:4px}.pagination>li{display:inline}.pagination>li>a,.pagination>li>span{position:relative;float:left;padding:6px 12px;line-height:1.42857143;text-decoration:none;color:#428bca;background-color:#fff;border:1px solid #ddd;margin-left:-1px}.pagination>li:first-child>a,.pagination>li:first-child>span{margin-left:0;border-bottom-left-radius:4px;border-top-left-radius:4px}.pagination>li:last-child>a,.pagination>li:last-child>span{border-bottom-right-radius:4px;border-top-right-radius:4px}.pagination>li>a:hover,.pagination>li>span:hover,.pagination>li>a:focus,.pagination>li>span:focus{color:#2a6496;background-color:#eee;border-color:#ddd}.pagination>.active>a,.pagination>.active>span,.pagination>.active>a:hover,.pagination>.active>span:hover,.pagination>.active>a:focus,.pagination>.active>span:focus{z-index:2;color:#fff;background-color:#428bca;border-color:#428bca;cursor:default}.pagination>.disabled>span,.pagination>.disabled>span:hover,.pagination>.disabled>span:focus,.pagination>.disabled>a,.pagination>.disabled>a:hover,.pagination>.disabled>a:focus{color:#999;background-color:#fff;border-color:#ddd;cursor:not-allowed}.pagination-lg>li>a,.pagination-lg>li>span{padding:10px 16px;font-size:18px}.pagination-lg>li:first-child>a,.pagination-lg>li:first-child>span{border-bottom-left-radius:6px;border-top-left-radius:6px}.pagination-lg>li:last-child>a,.pagination-lg>li:last-child>span{border-bottom-right-radius:6px;border-top-right-radius:6px}.pagination-sm>li>a,.pagination-sm>li>span{padding:5px 10px;font-size:12px}.pagination-sm>li:first-child>a,.pagination-sm>li:first-child>span{border-bottom-left-radius:3px;border-top-left-radius:3px}.pagination-sm>li:last-child>a,.pagination-sm>li:last-child>span{border-bottom-right-radius:3px;border-top-right-radius:3px}.pager{padding-left:0;margin:20px 0;list-style:none;text-align:center}.pager li{display:inline}.pager li>a,.pager li>span{display:inline-block;padding:5px 14px;background-color:#fff;border:1px solid #ddd;border-radius:15px}.pager li>a:hover,.pager li>a:focus{text-decoration:none;background-color:#eee}.pager .next>a,.pager .next>span{float:right}.pager .previous>a,.pager .previous>span{float:left}.pager .disabled>a,.pager .disabled>a:hover,.pager .disabled>a:focus,.pager .disabled>span{color:#999;background-color:#fff;cursor:not-allowed}.label{display:inline;padding:.2em .6em .3em;font-size:75%;font-weight:700;line-height:1;color:#fff;text-align:center;white-space:nowrap;vertical-align:baseline;border-radius:.25em}.label[href]:hover,.label[href]:focus{color:#fff;text-decoration:none;cursor:pointer}.label:empty{display:none}.btn .label{position:relative;top:-1px}.label-default{background-color:#999}.label-default[href]:hover,.label-default[href]:focus{background-color:gray}.label-primary{background-color:#428bca}.label-primary[href]:hover,.label-primary[href]:focus{background-color:#3071a9}.label-success{background-color:#5cb85c}.label-success[href]:hover,.label-success[href]:focus{background-color:#449d44}.label-info{background-color:#5bc0de}.label-info[href]:hover,.label-info[href]:focus{background-color:#31b0d5}.label-warning{background-color:#f0ad4e}.label-warning[href]:hover,.label-warning[href]:focus{background-color:#ec971f}.label-danger{background-color:#d9534f}.label-danger[href]:hover,.label-danger[href]:focus{background-color:#c9302c}.badge{display:inline-block;min-width:10px;padding:3px 7px;font-size:12px;font-weight:700;color:#fff;line-height:1;vertical-align:baseline;white-space:nowrap;text-align:center;background-color:#999;border-radius:10px}.badge:empty{display:none}.btn .badge{position:relative;top:-1px}.btn-xs .badge{top:0;padding:1px 5px}a.badge:hover,a.badge:focus{color:#fff;text-decoration:none;cursor:pointer}a.list-group-item.active>.badge,.nav-pills>.active>a>.badge{color:#428bca;background-color:#fff}.nav-pills>li>a>.badge{margin-left:3px}.jumbotron{padding:30px;margin-bottom:30px;color:inherit;background-color:#eee}.jumbotron h1,.jumbotron .h1{color:inherit}.jumbotron p{margin-bottom:15px;font-size:21px;font-weight:200}.container .jumbotron{border-radius:6px}.jumbotron .container{max-width:100%}@media screen and (min-width:768px){.jumbotron{padding-top:48px;padding-bottom:48px}.container .jumbotron{padding-left:60px;padding-right:60px}.jumbotron h1,.jumbotron .h1{font-size:63px}}.thumbnail{display:block;padding:4px;margin-bottom:20px;line-height:1.42857143;background-color:#fff;border:1px solid #ddd;border-radius:4px;-webkit-transition:all .2s ease-in-out;transition:all .2s ease-in-out}.thumbnail>img,.thumbnail a>img{margin-left:auto;margin-right:auto}a.thumbnail:hover,a.thumbnail:focus,a.thumbnail.active{border-color:#428bca}.thumbnail .caption{padding:9px;color:#333}.alert{padding:15px;margin-bottom:20px;border:1px solid transparent;border-radius:4px}.alert h4{margin-top:0;color:inherit}.alert .alert-link{font-weight:700}.alert>p,.alert>ul{margin-bottom:0}.alert>p+p{margin-top:5px}.alert-dismissable{padding-right:35px}.alert-dismissable .close{position:relative;top:-2px;right:-21px;color:inherit}.alert-success{background-color:#dff0d8;border-color:#d6e9c6;color:#3c763d}.alert-success hr{border-top-color:#c9e2b3}.alert-success .alert-link{color:#2b542c}.alert-info{background-color:#d9edf7;border-color:#bce8f1;color:#31708f}.alert-info hr{border-top-color:#a6e1ec}.alert-info .alert-link{color:#245269}.alert-warning{background-color:#fcf8e3;border-color:#faebcc;color:#8a6d3b}.alert-warning hr{border-top-color:#f7e1b5}.alert-warning .alert-link{color:#66512c}.alert-danger{background-color:#f2dede;border-color:#ebccd1;color:#a94442}.alert-danger hr{border-top-color:#e4b9c0}.alert-danger .alert-link{color:#843534}@-webkit-keyframes progress-bar-stripes{from{background-position:40px 0}to{background-position:0 0}}@keyframes progress-bar-stripes{from{background-position:40px 0}to{background-position:0 0}}.progress{overflow:hidden;height:20px;margin-bottom:20px;background-color:#f5f5f5;border-radius:4px;-webkit-box-shadow:inset 0 1px 2px rgba(0,0,0,.1);box-shadow:inset 0 1px 2px rgba(0,0,0,.1)}.progress-bar{float:left;width:0;height:100%;font-size:12px;line-height:20px;color:#fff;text-align:center;background-color:#428bca;-webkit-box-shadow:inset 0 -1px 0 rgba(0,0,0,.15);box-shadow:inset 0 -1px 0 rgba(0,0,0,.15);-webkit-transition:width .6s ease;transition:width .6s ease}.progress-striped .progress-bar{background-image:-webkit-linear-gradient(45deg,rgba(255,255,255,.15) 25%,transparent 25%,transparent 50%,rgba(255,255,255,.15) 50%,rgba(255,255,255,.15) 75%,transparent 75%,transparent);background-image:linear-gradient(45deg,rgba(255,255,255,.15) 25%,transparent 25%,transparent 50%,rgba(255,255,255,.15) 50%,rgba(255,255,255,.15) 75%,transparent 75%,transparent);background-size:40px 40px}.progress.active .progress-bar{-webkit-animation:progress-bar-stripes 2s linear infinite;animation:progress-bar-stripes 2s linear infinite}.progress-bar-success{background-color:#5cb85c}.progress-striped .progress-bar-success{background-image:-webkit-linear-gradient(45deg,rgba(255,255,255,.15) 25%,transparent 25%,transparent 50%,rgba(255,255,255,.15) 50%,rgba(255,255,255,.15) 75%,transparent 75%,transparent);background-image:linear-gradient(45deg,rgba(255,255,255,.15) 25%,transparent 25%,transparent 50%,rgba(255,255,255,.15) 50%,rgba(255,255,255,.15) 75%,transparent 75%,transparent)}.progress-bar-info{background-color:#5bc0de}.progress-striped .progress-bar-info{background-image:-webkit-linear-gradient(45deg,rgba(255,255,255,.15) 25%,transparent 25%,transparent 50%,rgba(255,255,255,.15) 50%,rgba(255,255,255,.15) 75%,transparent 75%,transparent);background-image:linear-gradient(45deg,rgba(255,255,255,.15) 25%,transparent 25%,transparent 50%,rgba(255,255,255,.15) 50%,rgba(255,255,255,.15) 75%,transparent 75%,transparent)}.progress-bar-warning{background-color:#f0ad4e}.progress-striped .progress-bar-warning{background-image:-webkit-linear-gradient(45deg,rgba(255,255,255,.15) 25%,transparent 25%,transparent 50%,rgba(255,255,255,.15) 50%,rgba(255,255,255,.15) 75%,transparent 75%,transparent);background-image:linear-gradient(45deg,rgba(255,255,255,.15) 25%,transparent 25%,transparent 50%,rgba(255,255,255,.15) 50%,rgba(255,255,255,.15) 75%,transparent 75%,transparent)}.progress-bar-danger{background-color:#d9534f}.progress-striped .progress-bar-danger{background-image:-webkit-linear-gradient(45deg,rgba(255,255,255,.15) 25%,transparent 25%,transparent 50%,rgba(255,255,255,.15) 50%,rgba(255,255,255,.15) 75%,transparent 75%,transparent);background-image:linear-gradient(45deg,rgba(255,255,255,.15) 25%,transparent 25%,transparent 50%,rgba(255,255,255,.15) 50%,rgba(255,255,255,.15) 75%,transparent 75%,transparent)}.media,.media-body{overflow:hidden;zoom:1}.media,.media .media{margin-top:15px}.media:first-child{margin-top:0}.media-object{display:block}.media-heading{margin:0 0 5px}.media>.pull-left{margin-right:10px}.media>.pull-right{margin-left:10px}.media-list{padding-left:0;list-style:none}.list-group{margin-bottom:20px;padding-left:0}.list-group-item{position:relative;display:block;padding:10px 15px;margin-bottom:-1px;background-color:#fff;border:1px solid #ddd}.list-group-item:first-child{border-top-right-radius:4px;border-top-left-radius:4px}.list-group-item:last-child{margin-bottom:0;border-bottom-right-radius:4px;border-bottom-left-radius:4px}.list-group-item>.badge{float:right}.list-group-item>.badge+.badge{margin-right:5px}a.list-group-item{color:#555}a.list-group-item .list-group-item-heading{color:#333}a.list-group-item:hover,a.list-group-item:focus{text-decoration:none;background-color:#f5f5f5}a.list-group-item.active,a.list-group-item.active:hover,a.list-group-item.active:focus{z-index:2;color:#fff;background-color:#428bca;border-color:#428bca}a.list-group-item.active .list-group-item-heading,a.list-group-item.active:hover .list-group-item-heading,a.list-group-item.active:focus .list-group-item-heading{color:inherit}a.list-group-item.active .list-group-item-text,a.list-group-item.active:hover .list-group-item-text,a.list-group-item.active:focus .list-group-item-text{color:#e1edf7}.list-group-item-success{color:#3c763d;background-color:#dff0d8}a.list-group-item-success{color:#3c763d}a.list-group-item-success .list-group-item-heading{color:inherit}a.list-group-item-success:hover,a.list-group-item-success:focus{color:#3c763d;background-color:#d0e9c6}a.list-group-item-success.active,a.list-group-item-success.active:hover,a.list-group-item-success.active:focus{color:#fff;background-color:#3c763d;border-color:#3c763d}.list-group-item-info{color:#31708f;background-color:#d9edf7}a.list-group-item-info{color:#31708f}a.list-group-item-info .list-group-item-heading{color:inherit}a.list-group-item-info:hover,a.list-group-item-info:focus{color:#31708f;background-color:#c4e3f3}a.list-group-item-info.active,a.list-group-item-info.active:hover,a.list-group-item-info.active:focus{color:#fff;background-color:#31708f;border-color:#31708f}.list-group-item-warning{color:#8a6d3b;background-color:#fcf8e3}a.list-group-item-warning{color:#8a6d3b}a.list-group-item-warning .list-group-item-heading{color:inherit}a.list-group-item-warning:hover,a.list-group-item-warning:focus{color:#8a6d3b;background-color:#faf2cc}a.list-group-item-warning.active,a.list-group-item-warning.active:hover,a.list-group-item-warning.active:focus{color:#fff;background-color:#8a6d3b;border-color:#8a6d3b}.list-group-item-danger{color:#a94442;background-color:#f2dede}a.list-group-item-danger{color:#a94442}a.list-group-item-danger .list-group-item-heading{color:inherit}a.list-group-item-danger:hover,a.list-group-item-danger:focus{color:#a94442;background-color:#ebcccc}a.list-group-item-danger.active,a.list-group-item-danger.active:hover,a.list-group-item-danger.active:focus{color:#fff;background-color:#a94442;border-color:#a94442}.list-group-item-heading{margin-top:0;margin-bottom:5px}.list-group-item-text{margin-bottom:0;line-height:1.3}.panel{margin-bottom:20px;background-color:#fff;border:1px solid transparent;border-radius:4px;-webkit-box-shadow:0 1px 1px rgba(0,0,0,.05);box-shadow:0 1px 1px rgba(0,0,0,.05)}.panel-body{padding:15px}.panel-heading{padding:10px 15px;border-bottom:1px solid transparent;border-top-right-radius:3px;border-top-left-radius:3px}.panel-heading>.dropdown .dropdown-toggle{color:inherit}.panel-title{margin-top:0;margin-bottom:0;font-size:16px;color:inherit}.panel-title>a{color:inherit}.panel-footer{padding:10px 15px;background-color:#f5f5f5;border-top:1px solid #ddd;border-bottom-right-radius:3px;border-bottom-left-radius:3px}.panel>.list-group{margin-bottom:0}.panel>.list-group .list-group-item{border-width:1px 0;border-radius:0}.panel>.list-group:first-child .list-group-item:first-child{border-top:0;border-top-right-radius:3px;border-top-left-radius:3px}.panel>.list-group:last-child .list-group-item:last-child{border-bottom:0;border-bottom-right-radius:3px;border-bottom-left-radius:3px}.panel-heading+.list-group .list-group-item:first-child{border-top-width:0}.panel>.table,.panel>.table-responsive>.table{margin-bottom:0}.panel>.table:first-child,.panel>.table-responsive:first-child>.table:first-child{border-top-right-radius:3px;border-top-left-radius:3px}.panel>.table:first-child>thead:first-child>tr:first-child td:first-child,.panel>.table-responsive:first-child>.table:first-child>thead:first-child>tr:first-child td:first-child,.panel>.table:first-child>tbody:first-child>tr:first-child td:first-child,.panel>.table-responsive:first-child>.table:first-child>tbody:first-child>tr:first-child td:first-child,.panel>.table:first-child>thead:first-child>tr:first-child th:first-child,.panel>.table-responsive:first-child>.table:first-child>thead:first-child>tr:first-child th:first-child,.panel>.table:first-child>tbody:first-child>tr:first-child th:first-child,.panel>.table-responsive:first-child>.table:first-child>tbody:first-child>tr:first-child th:first-child{border-top-left-radius:3px}.panel>.table:first-child>thead:first-child>tr:first-child td:last-child,.panel>.table-responsive:first-child>.table:first-child>thead:first-child>tr:first-child td:last-child,.panel>.table:first-child>tbody:first-child>tr:first-child td:last-child,.panel>.table-responsive:first-child>.table:first-child>tbody:first-child>tr:first-child td:last-child,.panel>.table:first-child>thead:first-child>tr:first-child th:last-child,.panel>.table-responsive:first-child>.table:first-child>thead:first-child>tr:first-child th:last-child,.panel>.table:first-child>tbody:first-child>tr:first-child th:last-child,.panel>.table-responsive:first-child>.table:first-child>tbody:first-child>tr:first-child th:last-child{border-top-right-radius:3px}.panel>.table:last-child,.panel>.table-responsive:last-child>.table:last-child{border-bottom-right-radius:3px;border-bottom-left-radius:3px}.panel>.table:last-child>tbody:last-child>tr:last-child td:first-child,.panel>.table-responsive:last-child>.table:last-child>tbody:last-child>tr:last-child td:first-child,.panel>.table:last-child>tfoot:last-child>tr:last-child td:first-child,.panel>.table-responsive:last-child>.table:last-child>tfoot:last-child>tr:last-child td:first-child,.panel>.table:last-child>tbody:last-child>tr:last-child th:first-child,.panel>.table-responsive:last-child>.table:last-child>tbody:last-child>tr:last-child th:first-child,.panel>.table:last-child>tfoot:last-child>tr:last-child th:first-child,.panel>.table-responsive:last-child>.table:last-child>tfoot:last-child>tr:last-child th:first-child{border-bottom-left-radius:3px}.panel>.table:last-child>tbody:last-child>tr:last-child td:last-child,.panel>.table-responsive:last-child>.table:last-child>tbody:last-child>tr:last-child td:last-child,.panel>.table:last-child>tfoot:last-child>tr:last-child td:last-child,.panel>.table-responsive:last-child>.table:last-child>tfoot:last-child>tr:last-child td:last-child,.panel>.table:last-child>tbody:last-child>tr:last-child th:last-child,.panel>.table-responsive:last-child>.table:last-child>tbody:last-child>tr:last-child th:last-child,.panel>.table:last-child>tfoot:last-child>tr:last-child th:last-child,.panel>.table-responsive:last-child>.table:last-child>tfoot:last-child>tr:last-child th:last-child{border-bottom-right-radius:3px}.panel>.panel-body+.table,.panel>.panel-body+.table-responsive{border-top:1px solid #ddd}.panel>.table>tbody:first-child>tr:first-child th,.panel>.table>tbody:first-child>tr:first-child td{border-top:0}.panel>.table-bordered,.panel>.table-responsive>.table-bordered{border:0}.panel>.table-bordered>thead>tr>th:first-child,.panel>.table-responsive>.table-bordered>thead>tr>th:first-child,.panel>.table-bordered>tbody>tr>th:first-child,.panel>.table-responsive>.table-bordered>tbody>tr>th:first-child,.panel>.table-bordered>tfoot>tr>th:first-child,.panel>.table-responsive>.table-bordered>tfoot>tr>th:first-child,.panel>.table-bordered>thead>tr>td:first-child,.panel>.table-responsive>.table-bordered>thead>tr>td:first-child,.panel>.table-bordered>tbody>tr>td:first-child,.panel>.table-responsive>.table-bordered>tbody>tr>td:first-child,.panel>.table-bordered>tfoot>tr>td:first-child,.panel>.table-responsive>.table-bordered>tfoot>tr>td:first-child{border-left:0}.panel>.table-bordered>thead>tr>th:last-child,.panel>.table-responsive>.table-bordered>thead>tr>th:last-child,.panel>.table-bordered>tbody>tr>th:last-child,.panel>.table-responsive>.table-bordered>tbody>tr>th:last-child,.panel>.table-bordered>tfoot>tr>th:last-child,.panel>.table-responsive>.table-bordered>tfoot>tr>th:last-child,.panel>.table-bordered>thead>tr>td:last-child,.panel>.table-responsive>.table-bordered>thead>tr>td:last-child,.panel>.table-bordered>tbody>tr>td:last-child,.panel>.table-responsive>.table-bordered>tbody>tr>td:last-child,.panel>.table-bordered>tfoot>tr>td:last-child,.panel>.table-responsive>.table-bordered>tfoot>tr>td:last-child{border-right:0}.panel>.table-bordered>thead>tr:first-child>td,.panel>.table-responsive>.table-bordered>thead>tr:first-child>td,.panel>.table-bordered>tbody>tr:first-child>td,.panel>.table-responsive>.table-bordered>tbody>tr:first-child>td,.panel>.table-bordered>thead>tr:first-child>th,.panel>.table-responsive>.table-bordered>thead>tr:first-child>th,.panel>.table-bordered>tbody>tr:first-child>th,.panel>.table-responsive>.table-bordered>tbody>tr:first-child>th{border-bottom:0}.panel>.table-bordered>tbody>tr:last-child>td,.panel>.table-responsive>.table-bordered>tbody>tr:last-child>td,.panel>.table-bordered>tfoot>tr:last-child>td,.panel>.table-responsive>.table-bordered>tfoot>tr:last-child>td,.panel>.table-bordered>tbody>tr:last-child>th,.panel>.table-responsive>.table-bordered>tbody>tr:last-child>th,.panel>.table-bordered>tfoot>tr:last-child>th,.panel>.table-responsive>.table-bordered>tfoot>tr:last-child>th{border-bottom:0}.panel>.table-responsive{border:0;margin-bottom:0}.panel-group{margin-bottom:20px}.panel-group .panel{margin-bottom:0;border-radius:4px;overflow:hidden}.panel-group .panel+.panel{margin-top:5px}.panel-group .panel-heading{border-bottom:0}.panel-group .panel-heading+.panel-collapse .panel-body{border-top:1px solid #ddd}.panel-group .panel-footer{border-top:0}.panel-group .panel-footer+.panel-collapse .panel-body{border-bottom:1px solid #ddd}.panel-default{border-color:#ddd}.panel-default>.panel-heading{color:#333;background-color:#f5f5f5;border-color:#ddd}.panel-default>.panel-heading+.panel-collapse .panel-body{border-top-color:#ddd}.panel-default>.panel-footer+.panel-collapse .panel-body{border-bottom-color:#ddd}.panel-primary{border-color:#428bca}.panel-primary>.panel-heading{color:#fff;background-color:#428bca;border-color:#428bca}.panel-primary>.panel-heading+.panel-collapse .panel-body{border-top-color:#428bca}.panel-primary>.panel-footer+.panel-collapse .panel-body{border-bottom-color:#428bca}.panel-success{border-color:#d6e9c6}.panel-success>.panel-heading{color:#3c763d;background-color:#dff0d8;border-color:#d6e9c6}.panel-success>.panel-heading+.panel-collapse .panel-body{border-top-color:#d6e9c6}.panel-success>.panel-footer+.panel-collapse .panel-body{border-bottom-color:#d6e9c6}.panel-info{border-color:#bce8f1}.panel-info>.panel-heading{color:#31708f;background-color:#d9edf7;border-color:#bce8f1}.panel-info>.panel-heading+.panel-collapse .panel-body{border-top-color:#bce8f1}.panel-info>.panel-footer+.panel-collapse .panel-body{border-bottom-color:#bce8f1}.panel-warning{border-color:#faebcc}.panel-warning>.panel-heading{color:#8a6d3b;background-color:#fcf8e3;border-color:#faebcc}.panel-warning>.panel-heading+.panel-collapse .panel-body{border-top-color:#faebcc}.panel-warning>.panel-footer+.panel-collapse .panel-body{border-bottom-color:#faebcc}.panel-danger{border-color:#ebccd1}.panel-danger>.panel-heading{color:#a94442;background-color:#f2dede;border-color:#ebccd1}.panel-danger>.panel-heading+.panel-collapse .panel-body{border-top-color:#ebccd1}.panel-danger>.panel-footer+.panel-collapse .panel-body{border-bottom-color:#ebccd1}.well{min-height:20px;padding:19px;margin-bottom:20px;background-color:#f5f5f5;border:1px solid #e3e3e3;border-radius:4px;-webkit-box-shadow:inset 0 1px 1px rgba(0,0,0,.05);box-shadow:inset 0 1px 1px rgba(0,0,0,.05)}.well blockquote{border-color:#ddd;border-color:rgba(0,0,0,.15)}.well-lg{padding:24px;border-radius:6px}.well-sm{padding:9px;border-radius:3px}.close{float:right;font-size:21px;font-weight:700;line-height:1;color:#000;text-shadow:0 1px 0 #fff;opacity:.2;filter:alpha(opacity=20)}.close:hover,.close:focus{color:#000;text-decoration:none;cursor:pointer;opacity:.5;filter:alpha(opacity=50)}button.close{padding:0;cursor:pointer;background:0 0;border:0;-webkit-appearance:none}.modal-open{overflow:hidden}.modal{display:none;overflow:auto;overflow-y:scroll;position:fixed;top:0;right:0;bottom:0;left:0;z-index:1050;-webkit-overflow-scrolling:touch;outline:0}.modal.fade .modal-dialog{-webkit-transform:translate(0,-25%);-ms-transform:translate(0,-25%);transform:translate(0,-25%);-webkit-transition:-webkit-transform .3s ease-out;-moz-transition:-moz-transform .3s ease-out;-o-transition:-o-transform .3s ease-out;transition:transform .3s ease-out}.modal.in .modal-dialog{-webkit-transform:translate(0,0);-ms-transform:translate(0,0);transform:translate(0,0)}.modal-dialog{position:relative;width:auto;margin:10px}.modal-content{position:relative;background-color:#fff;border:1px solid #999;border:1px solid rgba(0,0,0,.2);border-radius:6px;-webkit-box-shadow:0 3px 9px rgba(0,0,0,.5);box-shadow:0 3px 9px rgba(0,0,0,.5);background-clip:padding-box;outline:0}.modal-backdrop{position:fixed;top:0;right:0;bottom:0;left:0;z-index:1040;background-color:#000}.modal-backdrop.fade{opacity:0;filter:alpha(opacity=0)}.modal-backdrop.in{opacity:.5;filter:alpha(opacity=50)}.modal-header{padding:15px;border-bottom:1px solid #e5e5e5;min-height:16.42857143px}.modal-header .close{margin-top:-2px}.modal-title{margin:0;line-height:1.42857143}.modal-body{position:relative;padding:20px}.modal-footer{margin-top:15px;padding:19px 20px 20px;text-align:right;border-top:1px solid #e5e5e5}.modal-footer .btn+.btn{margin-left:5px;margin-bottom:0}.modal-footer .btn-group .btn+.btn{margin-left:-1px}.modal-footer .btn-block+.btn-block{margin-left:0}@media (min-width:768px){.modal-dialog{width:600px;margin:30px auto}.modal-content{-webkit-box-shadow:0 5px 15px rgba(0,0,0,.5);box-shadow:0 5px 15px rgba(0,0,0,.5)}.modal-sm{width:300px}}@media (min-width:992px){.modal-lg{width:900px}}.tooltip{position:absolute;z-index:1030;display:block;visibility:visible;font-size:12px;line-height:1.4;opacity:0;filter:alpha(opacity=0)}.tooltip.in{opacity:.9;filter:alpha(opacity=90)}.tooltip.top{margin-top:-3px;padding:5px 0}.tooltip.right{margin-left:3px;padding:0 5px}.tooltip.bottom{margin-top:3px;padding:5px 0}.tooltip.left{margin-left:-3px;padding:0 5px}.tooltip-inner{max-width:200px;padding:3px 8px;color:#fff;text-align:center;text-decoration:none;background-color:#000;border-radius:4px}.tooltip-arrow{position:absolute;width:0;height:0;border-color:transparent;border-style:solid}.tooltip.top .tooltip-arrow{bottom:0;left:50%;margin-left:-5px;border-width:5px 5px 0;border-top-color:#000}.tooltip.top-left .tooltip-arrow{bottom:0;left:5px;border-width:5px 5px 0;border-top-color:#000}.tooltip.top-right .tooltip-arrow{bottom:0;right:5px;border-width:5px 5px 0;border-top-color:#000}.tooltip.right .tooltip-arrow{top:50%;left:0;margin-top:-5px;border-width:5px 5px 5px 0;border-right-color:#000}.tooltip.left .tooltip-arrow{top:50%;right:0;margin-top:-5px;border-width:5px 0 5px 5px;border-left-color:#000}.tooltip.bottom .tooltip-arrow{top:0;left:50%;margin-left:-5px;border-width:0 5px 5px;border-bottom-color:#000}.tooltip.bottom-left .tooltip-arrow{top:0;left:5px;border-width:0 5px 5px;border-bottom-color:#000}.tooltip.bottom-right .tooltip-arrow{top:0;right:5px;border-width:0 5px 5px;border-bottom-color:#000}.popover{position:absolute;top:0;left:0;z-index:1010;display:none;max-width:276px;padding:1px;text-align:left;background-color:#fff;background-clip:padding-box;border:1px solid #ccc;border:1px solid rgba(0,0,0,.2);border-radius:6px;-webkit-box-shadow:0 5px 10px rgba(0,0,0,.2);box-shadow:0 5px 10px rgba(0,0,0,.2);white-space:normal}.popover.top{margin-top:-10px}.popover.right{margin-left:10px}.popover.bottom{margin-top:10px}.popover.left{margin-left:-10px}.popover-title{margin:0;padding:8px 14px;font-size:14px;font-weight:400;line-height:18px;background-color:#f7f7f7;border-bottom:1px solid #ebebeb;border-radius:5px 5px 0 0}.popover-content{padding:9px 14px}.popover>.arrow,.popover>.arrow:after{position:absolute;display:block;width:0;height:0;border-color:transparent;border-style:solid}.popover>.arrow{border-width:11px}.popover>.arrow:after{border-width:10px;content:\"\"}.popover.top>.arrow{left:50%;margin-left:-11px;border-bottom-width:0;border-top-color:#999;border-top-color:rgba(0,0,0,.25);bottom:-11px}.popover.top>.arrow:after{content:\" \";bottom:1px;margin-left:-10px;border-bottom-width:0;border-top-color:#fff}.popover.right>.arrow{top:50%;left:-11px;margin-top:-11px;border-left-width:0;border-right-color:#999;border-right-color:rgba(0,0,0,.25)}.popover.right>.arrow:after{content:\" \";left:1px;bottom:-10px;border-left-width:0;border-right-color:#fff}.popover.bottom>.arrow{left:50%;margin-left:-11px;border-top-width:0;border-bottom-color:#999;border-bottom-color:rgba(0,0,0,.25);top:-11px}.popover.bottom>.arrow:after{content:\" \";top:1px;margin-left:-10px;border-top-width:0;border-bottom-color:#fff}.popover.left>.arrow{top:50%;right:-11px;margin-top:-11px;border-right-width:0;border-left-color:#999;border-left-color:rgba(0,0,0,.25)}.popover.left>.arrow:after{content:\" \";right:1px;border-right-width:0;border-left-color:#fff;bottom:-10px}.carousel{position:relative}.carousel-inner{position:relative;overflow:hidden;width:100%}.carousel-inner>.item{display:none;position:relative;-webkit-transition:.6s ease-in-out left;transition:.6s ease-in-out left}.carousel-inner>.item>img,.carousel-inner>.item>a>img{line-height:1}.carousel-inner>.active,.carousel-inner>.next,.carousel-inner>.prev{display:block}.carousel-inner>.active{left:0}.carousel-inner>.next,.carousel-inner>.prev{position:absolute;top:0;width:100%}.carousel-inner>.next{left:100%}.carousel-inner>.prev{left:-100%}.carousel-inner>.next.left,.carousel-inner>.prev.right{left:0}.carousel-inner>.active.left{left:-100%}.carousel-inner>.active.right{left:100%}.carousel-control{position:absolute;top:0;left:0;bottom:0;width:15%;opacity:.5;filter:alpha(opacity=50);font-size:20px;color:#fff;text-align:center;text-shadow:0 1px 2px rgba(0,0,0,.6)}.carousel-control.left{background-image:-webkit-linear-gradient(left,color-stop(rgba(0,0,0,.5) 0),color-stop(rgba(0,0,0,.0001) 100%));background-image:linear-gradient(to right,rgba(0,0,0,.5) 0,rgba(0,0,0,.0001) 100%);background-repeat:repeat-x;filter:progid:DXImageTransform.Microsoft.gradient(startColorstr='#80000000', endColorstr='#00000000', GradientType=1)}.carousel-control.right{left:auto;right:0;background-image:-webkit-linear-gradient(left,color-stop(rgba(0,0,0,.0001) 0),color-stop(rgba(0,0,0,.5) 100%));background-image:linear-gradient(to right,rgba(0,0,0,.0001) 0,rgba(0,0,0,.5) 100%);background-repeat:repeat-x;filter:progid:DXImageTransform.Microsoft.gradient(startColorstr='#00000000', endColorstr='#80000000', GradientType=1)}.carousel-control:hover,.carousel-control:focus{outline:0;color:#fff;text-decoration:none;opacity:.9;filter:alpha(opacity=90)}.carousel-control .icon-prev,.carousel-control .icon-next,.carousel-control .glyphicon-chevron-left,.carousel-control .glyphicon-chevron-right{position:absolute;top:50%;z-index:5;display:inline-block}.carousel-control .icon-prev,.carousel-control .glyphicon-chevron-left{left:50%}.carousel-control .icon-next,.carousel-control .glyphicon-chevron-right{right:50%}.carousel-control .icon-prev,.carousel-control .icon-next{width:20px;height:20px;margin-top:-10px;margin-left:-10px;font-family:serif}.carousel-control .icon-prev:before{content:'\\2039'}.carousel-control .icon-next:before{content:'\\203a'}.carousel-indicators{position:absolute;bottom:10px;left:50%;z-index:15;width:60%;margin-left:-30%;padding-left:0;list-style:none;text-align:center}.carousel-indicators li{display:inline-block;width:10px;height:10px;margin:1px;text-indent:-999px;border:1px solid #fff;border-radius:10px;cursor:pointer;background-color:#000 \\9;background-color:rgba(0,0,0,0)}.carousel-indicators .active{margin:0;width:12px;height:12px;background-color:#fff}.carousel-caption{position:absolute;left:15%;right:15%;bottom:20px;z-index:10;padding-top:20px;padding-bottom:20px;color:#fff;text-align:center;text-shadow:0 1px 2px rgba(0,0,0,.6)}.carousel-caption .btn{text-shadow:none}@media screen and (min-width:768px){.carousel-control .glyphicon-chevron-left,.carousel-control .glyphicon-chevron-right,.carousel-control .icon-prev,.carousel-control .icon-next{width:30px;height:30px;margin-top:-15px;margin-left:-15px;font-size:30px}.carousel-caption{left:20%;right:20%;padding-bottom:30px}.carousel-indicators{bottom:20px}}.clearfix:before,.clearfix:after,.container:before,.container:after,.container-fluid:before,.container-fluid:after,.row:before,.row:after,.form-horizontal .form-group:before,.form-horizontal .form-group:after,.btn-toolbar:before,.btn-toolbar:after,.btn-group-vertical>.btn-group:before,.btn-group-vertical>.btn-group:after,.nav:before,.nav:after,.navbar:before,.navbar:after,.navbar-header:before,.navbar-header:after,.navbar-collapse:before,.navbar-collapse:after,.pager:before,.pager:after,.panel-body:before,.panel-body:after,.modal-footer:before,.modal-footer:after{content:\" \";display:table}.clearfix:after,.container:after,.container-fluid:after,.row:after,.form-horizontal .form-group:after,.btn-toolbar:after,.btn-group-vertical>.btn-group:after,.nav:after,.navbar:after,.navbar-header:after,.navbar-collapse:after,.pager:after,.panel-body:after,.modal-footer:after{clear:both}.center-block{display:block;margin-left:auto;margin-right:auto}.pull-right{float:right!important}.pull-left{float:left!important}.hide{display:none!important}.show{display:block!important}.invisible{visibility:hidden}.text-hide{font:0/0 a;color:transparent;text-shadow:none;background-color:transparent;border:0}.hidden{display:none!important;visibility:hidden!important}.affix{position:fixed}@-ms-viewport{width:device-width}.visible-xs,.visible-sm,.visible-md,.visible-lg{display:none!important}@media (max-width:767px){.visible-xs{display:block!important}table.visible-xs{display:table}tr.visible-xs{display:table-row!important}th.visible-xs,td.visible-xs{display:table-cell!important}}@media (min-width:768px) and (max-width:991px){.visible-sm{display:block!important}table.visible-sm{display:table}tr.visible-sm{display:table-row!important}th.visible-sm,td.visible-sm{display:table-cell!important}}@media (min-width:992px) and (max-width:1199px){.visible-md{display:block!important}table.visible-md{display:table}tr.visible-md{display:table-row!important}th.visible-md,td.visible-md{display:table-cell!important}}@media (min-width:1200px){.visible-lg{display:block!important}table.visible-lg{display:table}tr.visible-lg{display:table-row!important}th.visible-lg,td.visible-lg{display:table-cell!important}}@media (max-width:767px){.hidden-xs{display:none!important}}@media (min-width:768px) and (max-width:991px){.hidden-sm{display:none!important}}@media (min-width:992px) and (max-width:1199px){.hidden-md{display:none!important}}@media (min-width:1200px){.hidden-lg{display:none!important}}.visible-print{display:none!important}@media print{.visible-print{display:block!important}table.visible-print{display:table}tr.visible-print{display:table-row!important}th.visible-print,td.visible-print{display:table-cell!important}}@media print{.hidden-print{display:none!important}}"

/* shared memory */
static mem_t *contextstatsmem = NULL;
static mem_t *nodestatsmem = NULL;
static mem_t *hoststatsmem = NULL;
static mem_t *balancerstatsmem = NULL;
static mem_t *sessionidstatsmem = NULL;
static mem_t *domainstatsmem = NULL;
static mem_t *jgroupsidstatsmem = NULL;

static slotmem_storage_method *storage = NULL;
static balancer_method *balancerhandler = NULL;
static void (*advertise_info)(request_rec *) = NULL;

module AP_MODULE_DECLARE_DATA manager_module;

static char balancer_nonce[APR_UUID_FORMATTED_LENGTH + 1];

typedef struct mod_manager_config
{
    /* base name for the shared memory */
    char *basefilename;
    /* max number of context supported */
    int maxcontext;
    /* max number of node supported */
    int maxnode;
    /* max number of host supported */
    int maxhost;
    /* max number of session supported */
    int maxsessionid;
    /* max number of jgroupsid supported */
    int maxjgroupsid;

    /* version, the version is increased each time the node update logic is called */
    unsigned int tableversion;

    /* Should be the slotmem persisted (1) or not (0) */
    int persistent;

    /* check for nonce in the command logic */
    int nonce;

    /* default name for balancer */
    char *balancername;

    /* allow aditional display */
    int allow_display;
    /* allow command logic */
    int allow_cmd;
    /* don't context in first status page */
    int reduce_display;  
    /* maximum message size */
    int maxmesssize;
    /* Enable MCPM receiver */
    int enable_mcpm_receive;

} mod_manager_config;

/*
 * routines for the node_storage_method
 */
static apr_status_t loc_read_node(int ids, nodeinfo_t **node)
{
    return (get_node(nodestatsmem, node, ids));
}
static int loc_get_ids_used_node(int *ids)
{
    return(get_ids_used_node(nodestatsmem, ids)); 
}
static int loc_get_max_size_node()
{
    if (nodestatsmem)
        return(get_max_size_node(nodestatsmem));
    else
        return 0;
}
static int loc_get_max_size_jgroupsid()
{
    if (jgroupsidstatsmem)
        return(get_max_size_jgroupsid(jgroupsidstatsmem));
    else
        return 0;
}
static apr_status_t loc_remove_node(nodeinfo_t *node)
{
    return (remove_node(nodestatsmem, node));
}
static apr_status_t loc_find_node(nodeinfo_t **node, const char *route)
{
    return (find_node(nodestatsmem, node, route));
}

/* Check is the nodes (in shared memory) were modified since last
 * call to worker_nodes_are_updated().
 * return codes:
 *   0 : No update of the nodes since last time.
 *   x: The version has changed the local table need to be updated.
 */
static unsigned int loc_worker_nodes_need_update(void *data, apr_pool_t *pool)
{
    int size;
    server_rec *s = (server_rec *) data;
    unsigned int last = 0;
    mod_manager_config *mconf = ap_get_module_config(s->module_config, &manager_module);

    size = loc_get_max_size_node();
    if (size == 0)
        return 0; /* broken */

    last = get_version_node(nodestatsmem);

    if (last != mconf->tableversion)
        return last;
    return (0);
}
/* Store the last version update in the proccess config */
static int loc_worker_nodes_are_updated(void *data, unsigned int last)
{
    server_rec *s = (server_rec *) data;
    mod_manager_config *mconf = ap_get_module_config(s->module_config, &manager_module);
    mconf->tableversion = last;
    return (0);
}
static int loc_get_max_size_context()
{
    if (contextstatsmem)
        return(get_max_size_context(contextstatsmem));
    else
        return 0;
}
static int loc_get_max_size_host()
{
    if (hoststatsmem)
        return(get_max_size_host(hoststatsmem));
    else
        return 0;
}
/* Remove the virtual hosts and contexts corresponding the node */
static void loc_remove_host_context(int node, apr_pool_t *pool)
{
    /* for read the hosts */
    int i;
    int size = loc_get_max_size_host();
    int *id;
    int sizecontext = loc_get_max_size_context();
    int *idcontext;

    if (size == 0)
        return;
    id = apr_palloc(pool, sizeof(int) * size);
    idcontext = apr_palloc(pool, sizeof(int) * sizecontext);
    size = get_ids_used_host(hoststatsmem, id);
    for (i=0; i<size; i++) {
        hostinfo_t *ou;

        if (get_host(hoststatsmem, &ou, id[i]) != APR_SUCCESS)
            continue;
        if (ou->node == node)
            remove_host(hoststatsmem, ou);
    }

    sizecontext = get_ids_used_context(contextstatsmem, idcontext);
    for (i=0; i<sizecontext; i++) {
        contextinfo_t *context;
        if (get_context(contextstatsmem, &context, idcontext[i]) != APR_SUCCESS)
            continue;
        if (context->node == node)
            remove_context(contextstatsmem, context);
    }
}
static const struct node_storage_method node_storage =
{
    loc_read_node,
    loc_get_ids_used_node,
    loc_get_max_size_node,
    loc_worker_nodes_need_update,
    loc_worker_nodes_are_updated,
    loc_remove_node,
    loc_find_node,
    loc_remove_host_context,
};

/*
 * routines for the context_storage_method
 */
static apr_status_t loc_read_context(int ids, contextinfo_t **context)
{
    return (get_context(contextstatsmem, context, ids));
}
static int loc_get_ids_used_context(int *ids)
{
    return(get_ids_used_context(contextstatsmem, ids)); 
}
static void loc_lock_contexts()
{
    lock_contexts(contextstatsmem);
}
static void loc_unlock_contexts()
{
    unlock_contexts(contextstatsmem);
}
static const struct context_storage_method context_storage =
{
    loc_read_context,
    loc_get_ids_used_context,
    loc_get_max_size_context,
    loc_lock_contexts,
    loc_unlock_contexts
};

/*
 * routines for the host_storage_method
 */
static apr_status_t loc_read_host(int ids, hostinfo_t **host)
{
    return (get_host(hoststatsmem, host, ids));
}
static int loc_get_ids_used_host(int *ids)
{
    return(get_ids_used_host(hoststatsmem, ids)); 
}
static const struct host_storage_method host_storage =
{
    loc_read_host,
    loc_get_ids_used_host,
    loc_get_max_size_host
};

/*
 * routines for the balancer_storage_method
 */
static apr_status_t loc_read_balancer(int ids, balancerinfo_t **balancer)
{
    return (get_balancer(balancerstatsmem, balancer, ids));
}
static int loc_get_ids_used_balancer(int *ids)
{
    return(get_ids_used_balancer(balancerstatsmem, ids)); 
}
static int loc_get_max_size_balancer()
{
    if (balancerstatsmem)
        return(get_max_size_balancer(balancerstatsmem));
    else
        return 0;
}
static const struct balancer_storage_method balancer_storage =
{
    loc_read_balancer,
    loc_get_ids_used_balancer,
    loc_get_max_size_balancer
};
/*
 * routines for the sessionid_storage_method
 */
static apr_status_t loc_read_sessionid(int ids, sessionidinfo_t **sessionid)
{
    return (get_sessionid(sessionidstatsmem, sessionid, ids));
}
static int loc_get_ids_used_sessionid(int *ids)
{
    return(get_ids_used_sessionid(sessionidstatsmem, ids)); 
}
static int loc_get_max_size_sessionid()
{
    if (sessionidstatsmem)
        return(get_max_size_sessionid(sessionidstatsmem));
    else
        return 0;
}
static apr_status_t loc_remove_sessionid(sessionidinfo_t *sessionid)
{
    return (remove_sessionid(sessionidstatsmem, sessionid));
}
static apr_status_t loc_insert_update_sessionid(sessionidinfo_t *sessionid)
{
    return (insert_update_sessionid(sessionidstatsmem, sessionid));
}
static const struct  sessionid_storage_method sessionid_storage =
{
    loc_read_sessionid,
    loc_get_ids_used_sessionid,
    loc_get_max_size_sessionid,
    loc_remove_sessionid,
    loc_insert_update_sessionid
};

/*
 * routines for the domain_storage_method
 */
static apr_status_t loc_read_domain(int ids, domaininfo_t **domain)
{
    return (get_domain(domainstatsmem, domain, ids));
}
static int loc_get_ids_used_domain(int *ids)
{
    return(get_ids_used_domain(domainstatsmem, ids)); 
}
static int loc_get_max_size_domain()
{
    if (domainstatsmem)
        return(get_max_size_domain(domainstatsmem));
    else
        return 0;
}
static apr_status_t loc_remove_domain(domaininfo_t *domain)
{
    return (remove_domain(domainstatsmem, domain));
}
static apr_status_t loc_insert_update_domain(domaininfo_t *domain)
{
    return (insert_update_domain(domainstatsmem, domain));
}
static apr_status_t loc_find_domain(domaininfo_t **domain, const char *route, const char *balancer)
{
    return (find_domain(domainstatsmem, domain, route, balancer));
}
static const struct  domain_storage_method domain_storage =
{
    loc_read_domain,
    loc_get_ids_used_domain,
    loc_get_max_size_domain,
    loc_remove_domain,
    loc_insert_update_domain,
    loc_find_domain
};

/* helper for the handling of the Alias: host1,... Context: context1,... */
struct cluster_host {
    char *host;
    char *context;
    struct cluster_host *next;
};

/*
 * cleanup logic
 */
static apr_status_t cleanup_manager(void *param)
{
    /* shared memory */
    contextstatsmem = NULL;
    nodestatsmem = NULL;
    hoststatsmem = NULL;
    balancerstatsmem = NULL;
    sessionidstatsmem = NULL;
    domainstatsmem = NULL;
    jgroupsidstatsmem = NULL;
    return APR_SUCCESS;
}
static void mc_initialize_cleanup(apr_pool_t *p)
{
    apr_pool_cleanup_register(p, NULL, cleanup_manager, apr_pool_cleanup_null);
}

/*
 * call after parser the configuration.
 * create the shared memory.
 */
static int manager_init(apr_pool_t *p, apr_pool_t *plog,
                          apr_pool_t *ptemp, server_rec *s)
{
    char *node;
    char *context;
    char *host;
    char *balancer;
    char *sessionid;
    char *domain;
    char *jgroupsid;
    void *data;
    const char *userdata_key = "mod_manager_init";
    apr_uuid_t uuid;
    mod_manager_config *mconf = ap_get_module_config(s->module_config, &manager_module);
    apr_pool_userdata_get(&data, userdata_key, s->process->pool);
    if (!data) {
        /* first call do nothing */
        apr_pool_userdata_set((const void *)1, userdata_key, apr_pool_cleanup_null, s->process->pool);
        return OK;
    }

    if (mconf->basefilename) {
        node = apr_pstrcat(ptemp, mconf->basefilename, "/manager.node", NULL);
        context = apr_pstrcat(ptemp, mconf->basefilename, "/manager.context", NULL);
        host = apr_pstrcat(ptemp, mconf->basefilename, "/manager.host", NULL);
        balancer = apr_pstrcat(ptemp, mconf->basefilename, "/manager.balancer", NULL);
        sessionid = apr_pstrcat(ptemp, mconf->basefilename, "/manager.sessionid", NULL);
        domain = apr_pstrcat(ptemp, mconf->basefilename, "/manager.domain", NULL);
        jgroupsid = apr_pstrcat(ptemp, mconf->basefilename, "/manager.jgroupsid", NULL);
    } else {
        node = ap_server_root_relative(ptemp, "logs/manager.node");
        context = ap_server_root_relative(ptemp, "logs/manager.context");
        host = ap_server_root_relative(ptemp, "logs/manager.host");
        balancer = ap_server_root_relative(ptemp, "logs/manager.balancer");
        sessionid = ap_server_root_relative(ptemp, "logs/manager.sessionid");
        domain = ap_server_root_relative(ptemp, "logs/manager.domain");
        jgroupsid = ap_server_root_relative(ptemp, "logs/manager.jgroupsid");
    }

    /* Do some sanity checks */
    if (mconf->maxhost < mconf->maxnode)
        mconf->maxhost = mconf->maxnode;
    if (mconf->maxcontext < mconf->maxhost)
        mconf->maxcontext = mconf->maxhost;

    /* Get a provider to handle the shared memory */
    storage = ap_lookup_provider(SLOTMEM_STORAGE, "shared", "0");
    if (storage == NULL) {
        ap_log_error(APLOG_MARK, APLOG_NOERRNO|APLOG_EMERG, 0, s, "ap_lookup_provider %s failed", SLOTMEM_STORAGE);
        return  !OK;
    }
    nodestatsmem = create_mem_node(node, &mconf->maxnode, mconf->persistent, p, storage);
    if (nodestatsmem == NULL) {
        ap_log_error(APLOG_MARK, APLOG_NOERRNO|APLOG_EMERG, 0, s, "create_mem_node %s failed", node);
        return  !OK;
    }
    if (get_last_mem_error(nodestatsmem) != APR_SUCCESS) {
        char buf[120];
        ap_log_error(APLOG_MARK, APLOG_NOERRNO|APLOG_EMERG, 0, s, "create_mem_node %s failed: %s",
                     node, apr_strerror(get_last_mem_error(nodestatsmem), buf, sizeof(buf)));
        return  !OK;
    }

    contextstatsmem = create_mem_context(context, &mconf->maxcontext, mconf->persistent, p, storage);
    if (contextstatsmem == NULL) {
        ap_log_error(APLOG_MARK, APLOG_NOERRNO|APLOG_EMERG, 0, s, "create_mem_context failed");
        return  !OK;
    }

    hoststatsmem = create_mem_host(host, &mconf->maxhost, mconf->persistent, p, storage);
    if (hoststatsmem == NULL) {
        ap_log_error(APLOG_MARK, APLOG_NOERRNO|APLOG_EMERG, 0, s, "create_mem_host failed");
        return  !OK;
    }

    balancerstatsmem = create_mem_balancer(balancer, &mconf->maxhost, mconf->persistent, p, storage);
    if (balancerstatsmem == NULL) {
        ap_log_error(APLOG_MARK, APLOG_NOERRNO|APLOG_EMERG, 0, s, "create_mem_balancer failed");
        return  !OK;
    }

    if (mconf->maxsessionid) {
        /* Only create sessionid stuff if required */
        sessionidstatsmem = create_mem_sessionid(sessionid, &mconf->maxsessionid, mconf->persistent, p, storage);
        if (sessionidstatsmem == NULL) {
            ap_log_error(APLOG_MARK, APLOG_NOERRNO|APLOG_EMERG, 0, s, "create_mem_sessionid failed");
            return  !OK;
        }
    }

    domainstatsmem = create_mem_domain(domain, &mconf->maxnode, mconf->persistent, p, storage);
    if (domainstatsmem == NULL) {
        ap_log_error(APLOG_MARK, APLOG_NOERRNO|APLOG_EMERG, 0, s, "create_mem_domain failed");
        return  !OK;
    }

    if (mconf->maxjgroupsid) {
        /* Only create jgroupsid stuff if required */
        jgroupsidstatsmem = create_mem_jgroupsid(jgroupsid, &mconf->maxjgroupsid, mconf->persistent, p, storage);
        if (jgroupsidstatsmem == NULL) {
            ap_log_error(APLOG_MARK, APLOG_NOERRNO|APLOG_EMERG, 0, s, "create_mem_jgroupsid failed");
            return  !OK;
        }
    }

    /* Get a provider to ping/pong logics */

    balancerhandler = ap_lookup_provider("proxy_cluster", "balancer", "0");
    if (balancerhandler == NULL) {
        ap_log_error(APLOG_MARK, APLOG_NOERRNO|APLOG_WARNING, 0, s, "can't find a ping/pong logic");
    }

    advertise_info = ap_lookup_provider("advertise", "info", "0");

    /*
     * Retrieve a UUID and store the nonce.
     */
    apr_uuid_get(&uuid);
    apr_uuid_format(balancer_nonce, &uuid);

    /*
     * clean up to prevent backgroup thread (proxy_cluster_watchdog_func) to crash
     */
    mc_initialize_cleanup(p);

    return OK;
}
static char **process_buff(request_rec *r, char *buff)
{
    int i = 0;
    char *s = buff;
    char **ptr = NULL;
    for (; *s != '\0'; s++) {
        if (*s == '&' || *s == '=') {
            i++;
        }
    }
    ptr = apr_palloc(r->pool, sizeof(char *) * (i + 2));
    if (ptr == NULL)
        return NULL;

    s = buff;
    ptr[0] = s;
    ptr[i+1] = NULL;
    i = 1;
    for (; *s != '\0'; s++) {
        if (*s == '&' || *s == '=') {
            *s = '\0';
            ptr[i] = s + 1;
            i++;
        }
    }
    return ptr;
}
/*
 * Insert the hosts from Alias information
 */
static apr_status_t  insert_update_hosts(mem_t *mem, char *str, int node, int vhost)
{
    char *ptr = str;
    char *previous = str;
    hostinfo_t info;
    char empty[1] = {'\0'};
    apr_status_t status;

    info.node = node;
    info.vhost = vhost;
    if (ptr == NULL) {
        ptr = empty;
        previous = ptr;
    }
    while (*ptr) {
        if (*ptr == ',') {
            *ptr = '\0';
            strncpy(info.host, previous, sizeof(info.host));
            status = insert_update_host(mem, &info); 
            if (status != APR_SUCCESS)
                return status;
            previous = ptr + 1;
        }
        ptr ++;
    }
    strncpy(info.host, previous, sizeof(info.host));
    return insert_update_host(mem, &info); 
}
/*
 * Insert the context from Context information
 * Note:
 * 1 - if status is REMOVE remove_context will be called.
 * 2 - return codes of REMOVE are ignored (always success).
 *
 */
static apr_status_t  insert_update_contexts(mem_t *mem, char *str, int node, int vhost, int status)
{
    char *ptr = str;
    char *previous = str;
    apr_status_t ret = APR_SUCCESS;
    contextinfo_t info;
    char empty[2] = {'/','\0'};

    info.node = node;
    info.vhost = vhost;
    info.status = status;
    if (ptr == NULL) {
        ptr = empty;
        previous = ptr;
    }
    while (*ptr) {
        if (*ptr == ',') {
            *ptr = '\0';
            info.id = 0;
            strncpy(info.context, previous, sizeof(info.context));
            if (status != REMOVE) {
                ret = insert_update_context(mem, &info);
                if (ret != APR_SUCCESS)
                    return ret;
            } else
                remove_context(mem, &info);

            previous = ptr + 1;
        }
        ptr ++;
    }
    info.id = 0;
    strncpy(info.context, previous, sizeof(info.context));
    if (status != REMOVE)
        ret = insert_update_context(mem, &info); 
    else
        remove_context(mem, &info);
    return ret;
}
/*
 * Check that the node could be handle as is there were the same.
 */
static int  is_same_node(nodeinfo_t *nodeinfo, nodeinfo_t *node) {
    if (strcmp(nodeinfo->mess.balancer,node->mess.balancer))
        return 0;
    if (strcmp(nodeinfo->mess.Host, node->mess.Host))
        return 0;
    if (strcmp(nodeinfo->mess.Port,node->mess.Port))
        return 0;
    if (strcmp(nodeinfo->mess.Type, node->mess.Type))
        return 0;
    if (nodeinfo->mess.reversed != node->mess.reversed)
        return 0;

    /* Those means the reslist has to be changed */
    if (nodeinfo->mess.smax !=  node->mess.smax)
        return 0;
    if (nodeinfo->mess.ttl != node->mess.ttl)
        return 0;

    /* All other fields can be modified without causing problems */
    return -1;
}

/*
 * Process a CONFIG message
 * Balancer: <Balancer name>
 * <balancer configuration>
 * StickySession	StickySessionCookie	StickySessionPath	StickySessionRemove
 * StickySessionForce	Timeout	Maxattempts
 * JvmRoute?: <JvmRoute>
 * Domain: <Domain>
 * <Host: <Node IP>
 * Port: <Connector Port>
 * Type: <Type of the connector>
 * Reserved: <Use connection pool initiated by Tomcat *.>
 * <node conf>
 * flushpackets	flushwait	ping	smax	ttl
 * Virtual hosts in JBossAS
 * Alias: <vhost list>
 * Context corresponding to the applications.
 * Context: <context list>
 */
static char * process_config(request_rec *r, char **ptr, int *errtype)
{
    /* Process the node/balancer description */
    nodeinfo_t nodeinfo;
    nodeinfo_t *node;
    balancerinfo_t balancerinfo;
    int mpm_threads;
    
    struct cluster_host *vhost; 
    struct cluster_host *phost; 

    int i = 0;
    int id;
    int vid = 1; /* zero and "" is empty */
    void *sconf = r->server->module_config;
    mod_manager_config *mconf = ap_get_module_config(sconf, &manager_module);

    vhost = apr_palloc(r->pool, sizeof(struct cluster_host));

    /* Map nothing by default */
    vhost->host = NULL;
    vhost->context = NULL;
    vhost->next = NULL;
    phost = vhost;

    /* Fill default nodes values */
    memset(&nodeinfo.mess, '\0', sizeof(nodeinfo.mess));
    if (mconf->balancername != NULL) {
        strcpy(nodeinfo.mess.balancer, mconf->balancername);
    } else {
        strcpy(nodeinfo.mess.balancer, "mycluster");
    }
    strcpy(nodeinfo.mess.Host, "localhost");
    strcpy(nodeinfo.mess.Port, "8009");
    strcpy(nodeinfo.mess.Type, "ajp");
    nodeinfo.mess.reversed = 0;
    nodeinfo.mess.remove = 0; /* not marked as removed */
    nodeinfo.mess.flushpackets = flush_off; /* FLUSH_OFF; See enum flush_packets in proxy.h flush_off */
    nodeinfo.mess.flushwait = PROXY_FLUSH_WAIT;
    nodeinfo.mess.ping = apr_time_from_sec(10);
    ap_mpm_query(AP_MPMQ_MAX_THREADS, &mpm_threads);
    nodeinfo.mess.smax = mpm_threads + 1;
    nodeinfo.mess.ttl = apr_time_from_sec(60);
    nodeinfo.mess.timeout = 0;
    nodeinfo.mess.id = 0;
    nodeinfo.mess.lastcleantry = 0;

    /* Fill default balancer values */
    memset(&balancerinfo, '\0', sizeof(balancerinfo));
    if (mconf->balancername != NULL) {
        strcpy(balancerinfo.balancer, mconf->balancername);
    } else {
        strcpy(balancerinfo.balancer, "mycluster");
    }
    balancerinfo.StickySession = 1;
    balancerinfo.StickySessionForce = 1;
    strcpy(balancerinfo.StickySessionCookie, "JSESSIONID");
    strcpy(balancerinfo.StickySessionPath, "jsessionid");
    balancerinfo.Maxattempts = 1;
    balancerinfo.Timeout = 0;

    while (ptr[i]) {
        /* XXX: balancer part */
        if (strcasecmp(ptr[i], "Balancer") == 0) {
            if (strlen(ptr[i+1])>=sizeof(nodeinfo.mess.balancer)) {
                *errtype = TYPESYNTAX;
                return SBALBIG;
            }
            strcpy(nodeinfo.mess.balancer, ptr[i+1]);
            strcpy(balancerinfo.balancer, ptr[i+1]);
        }
        if (strcasecmp(ptr[i], "StickySession") == 0) {
            if (strcasecmp(ptr[i+1], "no") == 0)
                balancerinfo.StickySession = 0;
        }
        if (strcasecmp(ptr[i], "StickySessionCookie") == 0) {
            if (strlen(ptr[i+1])>=sizeof(balancerinfo.StickySessionCookie)) {
                *errtype = TYPESYNTAX;
                return SBAFBIG;
            }
            strcpy(balancerinfo.StickySessionCookie, ptr[i+1]);
        }
        if (strcasecmp(ptr[i], "StickySessionPath") == 0) {
            if (strlen(ptr[i+1])>=sizeof(balancerinfo.StickySessionPath)) {
                *errtype = TYPESYNTAX;
                return SBAFBIG;
            }
            strcpy(balancerinfo.StickySessionPath, ptr[i+1]);
        }
        if (strcasecmp(ptr[i], "StickySessionRemove") == 0) {
            if (strcasecmp(ptr[i+1], "yes") == 0)
                balancerinfo.StickySessionRemove = 1;
        }
        if (strcasecmp(ptr[i], "StickySessionForce") == 0) {
            if (strcasecmp(ptr[i+1], "no") == 0)
                balancerinfo.StickySessionForce = 0;
        }
        /* Note that it is workerTimeout (set/getWorkerTimeout in java code) */ 
        if (strcasecmp(ptr[i], "WaitWorker") == 0) {
            balancerinfo.Timeout = apr_time_from_sec(atoi(ptr[i+1]));
        }
        if (strcasecmp(ptr[i], "Maxattempts") == 0) {
            balancerinfo.Maxattempts = atoi(ptr[i+1]);
        }

        /* XXX: Node part */
        if (strcasecmp(ptr[i], "JVMRoute") == 0) {
            if (strlen(ptr[i+1])>=sizeof(nodeinfo.mess.JVMRoute)) {
                *errtype = TYPESYNTAX;
                return SROUBIG;
            }
            strcpy(nodeinfo.mess.JVMRoute, ptr[i+1]);
        }
        /* We renamed it LBGroup */
        if (strcasecmp(ptr[i], "Domain") == 0) {
            if (strlen(ptr[i+1])>=sizeof(nodeinfo.mess.Domain)) {
                *errtype = TYPESYNTAX;
                return SDOMBIG;
            }
            strcpy(nodeinfo.mess.Domain, ptr[i+1]);
        }
        if (strcasecmp(ptr[i], "Host") == 0) {
            char *p_read = ptr[i+1], *p_write = ptr[i+1];
            int flag = 0;
            if (strlen(ptr[i+1])>=sizeof(nodeinfo.mess.Host)) {
                *errtype = TYPESYNTAX;
                return SHOSBIG;
            }

            /* Removes %zone from an address */
            if (*p_read == '[') {
                while (*p_read) {
                    *p_write = *p_read++;
                    if ((*p_write == '%' || flag) && *p_write != ']') {
                        flag = 1;
                    } else {
                        p_write++;
                    }
                }
                *p_write = '\0';
            }

            strcpy(nodeinfo.mess.Host, ptr[i+1]);
        }
        if (strcasecmp(ptr[i], "Port") == 0) {
            if (strlen(ptr[i+1])>=sizeof(nodeinfo.mess.Port)) {
                *errtype = TYPESYNTAX;
                return SPORBIG;
            }
            strcpy(nodeinfo.mess.Port, ptr[i+1]);
        }
        if (strcasecmp(ptr[i], "Type") == 0) {
            if (strlen(ptr[i+1])>=sizeof(nodeinfo.mess.Type)) {
                *errtype = TYPESYNTAX;
                return STYPBIG;
            }
            strcpy(nodeinfo.mess.Type, ptr[i+1]);
        }
        if (strcasecmp(ptr[i], "Reversed") == 0) {
            if (strcasecmp(ptr[i+1], "yes") == 0) {
            nodeinfo.mess.reversed = 1;
            }
        }
        if (strcasecmp(ptr[i], "flushpackets") == 0) {
            if (strcasecmp(ptr[i+1], "on") == 0) {
                nodeinfo.mess.flushpackets = flush_on;
            }
            else if (strcasecmp(ptr[i+1], "auto") == 0) {
                nodeinfo.mess.flushpackets = flush_auto;
            }
        }
        if (strcasecmp(ptr[i], "flushwait") == 0) {
            nodeinfo.mess.flushwait = atoi(ptr[i+1]) * 1000;
        }
        if (strcasecmp(ptr[i], "ping") == 0) {
            nodeinfo.mess.ping = apr_time_from_sec(atoi(ptr[i+1]));
        }
        if (strcasecmp(ptr[i], "smax") == 0) {
            nodeinfo.mess.smax = atoi(ptr[i+1]);
        }
        if (strcasecmp(ptr[i], "ttl") == 0) {
            nodeinfo.mess.ttl = apr_time_from_sec(atoi(ptr[i+1]));
        }
        if (strcasecmp(ptr[i], "Timeout") == 0) {
            nodeinfo.mess.timeout = apr_time_from_sec(atoi(ptr[i+1]));
        }

        /* Hosts and contexts (optional paramters) */
        if (strcasecmp(ptr[i], "Alias") == 0) {
            if (phost->host && !phost->context) {
                *errtype = TYPESYNTAX;
                return SALIBAD;
            }
            if (phost->host) {
               phost->next = apr_palloc(r->pool, sizeof(struct cluster_host));
               phost = phost->next;
               phost->next = NULL;
               phost->host = ptr[i+1];
               phost->context = NULL;
            } else {
               phost->host = ptr[i+1];
            }
        }
        if (strcasecmp(ptr[i], "Context") == 0) {
            if (phost->context) {
                *errtype = TYPESYNTAX;
                return SCONBAD;
            }
            phost->context = ptr[i+1];
        }
        i++;
        i++;
    }

    /* Check for JVMRoute */
    if (nodeinfo.mess.JVMRoute[0] == '\0') {
        *errtype = TYPESYNTAX;
        return SROUBAD;
    }

    /* Insert or update balancer description */
    if (insert_update_balancer(balancerstatsmem, &balancerinfo) != APR_SUCCESS) {
        *errtype = TYPEMEM;
        return MBALAUI;
    }

    /* check for removed node */
    node = read_node(nodestatsmem, &nodeinfo);
    if (node != NULL) {
        /* If the node is removed (or kill and restarted) and recreated unchanged that is ok: network problems */
        if (! is_same_node(node, &nodeinfo)) {
            /* Here we can't update it because the old one is still in */
            ap_log_error(APLOG_MARK, APLOG_DEBUG, 0, r->server,
                         "process_config: node %s already exist", node->mess.JVMRoute);
            strcpy(node->mess.JVMRoute, "REMOVED");
            node->mess.remove = 1;
            insert_update_node(nodestatsmem, node, &id);
            loc_remove_host_context(node->mess.id, r->pool);
            *errtype = TYPEMEM;
            return MNODERM;
        }
    }

    /* Insert or update node description */
    if (insert_update_node(nodestatsmem, &nodeinfo, &id) != APR_SUCCESS) {
        *errtype = TYPEMEM;
        return MNODEUI;
    }

    /* Insert the Alias and corresponding Context */
    phost = vhost;
    if (phost->host == NULL && phost->context == NULL)
        return NULL; /* Alias and Context missing */
    while (phost) {
        if (insert_update_hosts(hoststatsmem, phost->host, id, vid) != APR_SUCCESS)
            return MHOSTUI;
        if (insert_update_contexts(contextstatsmem, phost->context, id, vid, STOPPED) != APR_SUCCESS)
            return MCONTUI;
        phost = phost->next;
        vid++;
    }
    return NULL;
}
/*
 * Process a DUMP command.
 */
static char * process_dump(request_rec *r, int *errtype)
{
    int size, i;
    int *id;

    ap_set_content_type(r, "text/plain");

    size = loc_get_max_size_balancer();
    if (size == 0)
       return NULL;
    id = apr_palloc(r->pool, sizeof(int) * size);
    size = get_ids_used_balancer(balancerstatsmem, id);
    for (i=0; i<size; i++) {
        balancerinfo_t *ou;
        if (get_balancer(balancerstatsmem, &ou, id[i]) != APR_SUCCESS)
            continue;
        ap_rprintf(r, "balancer: [%d] Name: %.*s Sticky: %d [%.*s]/[%.*s] remove: %d force: %d Timeout: %d maxAttempts: %d\n",
                   id[i], (int) sizeof(ou->balancer), ou->balancer, ou->StickySession,
                   (int) sizeof(ou->StickySessionCookie), ou->StickySessionCookie, (int) sizeof(ou->StickySessionPath), ou->StickySessionPath,
                   ou->StickySessionRemove, ou->StickySessionForce,
                   (int) apr_time_sec(ou->Timeout),
                   ou->Maxattempts);
    }

    size = loc_get_max_size_node();
    id = apr_palloc(r->pool, sizeof(int) * size);
    size = get_ids_used_node(nodestatsmem, id);
    for (i=0; i<size; i++) {
        nodeinfo_t *ou;
        if (get_node(nodestatsmem, &ou, id[i]) != APR_SUCCESS)
            continue;
        ap_rprintf(r, "node: [%d:%d],Balancer: %.*s,JVMRoute: %.*s,LBGroup: [%.*s],Host: %.*s,Port: %.*s,Type: %.*s,flushpackets: %d,flushwait: %d,ping: %d,smax: %d,ttl: %d,timeout: %d\n",
                   id[i], ou->mess.id,
                   (int) sizeof(ou->mess.balancer), ou->mess.balancer,
                   (int) sizeof(ou->mess.JVMRoute), ou->mess.JVMRoute,
                   (int) sizeof(ou->mess.Domain), ou->mess.Domain,
                   (int) sizeof(ou->mess.Host), ou->mess.Host,
                   (int) sizeof(ou->mess.Port), ou->mess.Port,
                   (int) sizeof(ou->mess.Type), ou->mess.Type,
                   ou->mess.flushpackets, ou->mess.flushwait/1000, (int) apr_time_sec(ou->mess.ping), ou->mess.smax,
                   (int) apr_time_sec(ou->mess.ttl), (int) apr_time_sec(ou->mess.timeout));
    }

    size = loc_get_max_size_host();
    id = apr_palloc(r->pool, sizeof(int) * size);
    size = get_ids_used_host(hoststatsmem, id);
    for (i=0; i<size; i++) {
        hostinfo_t *ou;
        if (get_host(hoststatsmem, &ou, id[i]) != APR_SUCCESS)
            continue;
        ap_rprintf(r, "host: %d [%.*s] vhost: %d node: %d\n", id[i], (int) sizeof(ou->host), ou->host, ou->vhost,
                  ou->node);
    }

    size = loc_get_max_size_context();
    id = apr_palloc(r->pool, sizeof(int) * size);
    size = get_ids_used_context(contextstatsmem, id);
    for (i=0; i<size; i++) {
        contextinfo_t *ou;
        if (get_context(contextstatsmem, &ou, id[i]) != APR_SUCCESS)
            continue;
        ap_rprintf(r, "context: %d [%.*s] vhost: %d node: %d status: %d\n", id[i],
                   (int) sizeof(ou->context), ou->context,
                   ou->vhost, ou->node,
                   ou->status);
    }
    return NULL;
}
/*
 * Process a INFO command.
 * Statics informations ;-)
 */
static char * process_info(request_rec *r, int *errtype)
{
    int size, i;
    int *id;

    ap_set_content_type(r, "text/plain");

    size = loc_get_max_size_node();
    if (size == 0)
        return NULL;
    id = apr_palloc(r->pool, sizeof(int) * size);
    size = get_ids_used_node(nodestatsmem, id);
    for (i=0; i<size; i++) {
        nodeinfo_t *ou;
#if AP_MODULE_MAGIC_AT_LEAST(20101223,1)
        proxy_worker_shared *proxystat;
#else
        proxy_worker_stat *proxystat;
#endif
        char *flushpackets;
        char *pptr;
        if (get_node(nodestatsmem, &ou, id[i]) != APR_SUCCESS)
            continue;
        ap_rprintf(r, "Node: [%d],Name: %.*s,Balancer: %.*s,LBGroup: %.*s,Host: %.*s,Port: %.*s,Type: %.*s",
                   id[i],
                   (int) sizeof(ou->mess.JVMRoute), ou->mess.JVMRoute,
                   (int) sizeof(ou->mess.balancer), ou->mess.balancer,
                   (int) sizeof(ou->mess.Domain), ou->mess.Domain,
                   (int) sizeof(ou->mess.Host), ou->mess.Host,
                   (int) sizeof(ou->mess.Port), ou->mess.Port,
                   (int) sizeof(ou->mess.Type), ou->mess.Type);
        flushpackets = "Off";
        switch (ou->mess.flushpackets) {
            case flush_on:
                flushpackets = "On";
                break;
            case flush_auto:
                flushpackets = "Auto";
        }
        ap_rprintf(r, ",Flushpackets: %s,Flushwait: %d,Ping: %d,Smax: %d,Ttl: %d",
                   flushpackets, ou->mess.flushwait/1000,
                   (int) apr_time_sec(ou->mess.ping),
                   ou->mess.smax,
                   (int) apr_time_sec(ou->mess.ttl));
        pptr = (char *) ou;
        pptr = pptr + ou->offset;
#if AP_MODULE_MAGIC_AT_LEAST(20101223,1)
        proxystat  = (proxy_worker_shared *) pptr;
#else
        proxystat  = (proxy_worker_stat *) pptr;
#endif
        ap_rprintf(r, ",Elected: %d,Read: %d,Transfered: %d,Connected: %d,Load: %d\n",
                   (int) proxystat->elected, (int) proxystat->read, (int) proxystat->transferred,
                   (int) proxystat->busy, proxystat->lbfactor);
        
    }

    /* Process the Vhosts */
    size = loc_get_max_size_host();
    id = apr_palloc(r->pool, sizeof(int) * size);
    size = get_ids_used_host(hoststatsmem, id);
    for (i=0; i<size; i++) {
        hostinfo_t *ou;
        if (get_host(hoststatsmem, &ou, id[i]) != APR_SUCCESS)
            continue;
        ap_rprintf(r, "Vhost: [%d:%d:%d], Alias: %.*s\n",
                   ou->node, ou->vhost, id[i], (int ) sizeof(ou->host), ou->host);
    }

    /* Process the Contexts */
    size = loc_get_max_size_context();
    id = apr_palloc(r->pool, sizeof(int) * size);
    size = get_ids_used_context(contextstatsmem, id);
    for (i=0; i<size; i++) {
        contextinfo_t *ou;
        char *status;
        if (get_context(contextstatsmem, &ou, id[i]) != APR_SUCCESS)
            continue;
        status = "REMOVED";
        switch (ou->status) {
            case ENABLED:
                status = "ENABLED";
                break;
            case DISABLED:
                status = "DISABLED";
                break;
            case STOPPED:
                status = "STOPPED";
                break;
        }
        ap_rprintf(r, "Context: [%d:%d:%d], Context: %.*s, Status: %s\n",
                   ou->node, ou->vhost, id[i],
                   (int) sizeof(ou->context), ou->context,
                   status);
    }
    return NULL;
}

/* Process a *-APP command that applies to the node */
static char * process_node_cmd(request_rec *r, int status, int *errtype, nodeinfo_t *node)
{
    /* for read the hosts */
    int i,j;
    int size = loc_get_max_size_host();
    int *id;

    ap_log_error(APLOG_MARK, APLOG_DEBUG, 0, r->server,
                "process_node_cmd %d processing node: %d", status, node->mess.id);
    if (size == 0)
        return NULL;
    id = apr_palloc(r->pool, sizeof(int) * size);
    size = get_ids_used_host(hoststatsmem, id);
    for (i=0; i<size; i++) {
        hostinfo_t *ou;
        int sizecontext;
        int *idcontext;

        if (get_host(hoststatsmem, &ou, id[i]) != APR_SUCCESS)
            continue;
        if (ou->node != node->mess.id)
            continue;
        /* If the host corresponds to a node process all contextes */
        sizecontext = get_max_size_context(contextstatsmem);
        idcontext = apr_palloc(r->pool, sizeof(int) * sizecontext);
        sizecontext = get_ids_used_context(contextstatsmem, idcontext);
        for (j=0; j<sizecontext; j++) {
            contextinfo_t *context;
            if (get_context(contextstatsmem, &context, idcontext[j]) != APR_SUCCESS)
                continue;
            if (context->vhost == ou->vhost &&
                context->node == ou->node) {
                /* Process the context */
                if (status != REMOVE) {
                    context->status = status;
                    insert_update_context(contextstatsmem, context);
                } else
                    remove_context(contextstatsmem, context);

            }
        }
        if (status == REMOVE) {
            remove_host(hoststatsmem, ou);
        }
    }

    /* The REMOVE-APP * removes the node (well mark it removed) */
    if (status == REMOVE) {
        int id;
        node->mess.remove = 1;
        insert_update_node(nodestatsmem, node, &id);
    }
    return NULL;

}

/* Process an enable/disable/stop/remove application message */
static char * process_appl_cmd(request_rec *r, char **ptr, int status, int *errtype, int global, int fromnode)
{
    nodeinfo_t nodeinfo;
    nodeinfo_t *node;
    struct cluster_host *vhost;

    int i = 0;
    hostinfo_t hostinfo;
    hostinfo_t *host;
    
    memset(&nodeinfo.mess, '\0', sizeof(nodeinfo.mess));
    /* Map nothing by default */
    vhost = apr_palloc(r->pool, sizeof(struct cluster_host));
    vhost->host = NULL;
    vhost->context = NULL;
    vhost->next = NULL;

    while (ptr[i]) {
        if (strcasecmp(ptr[i], "JVMRoute") == 0) {
            if (strlen(ptr[i+1])>=sizeof(nodeinfo.mess.JVMRoute)) {
                *errtype = TYPESYNTAX;
                return SROUBIG;
            }
            strcpy(nodeinfo.mess.JVMRoute, ptr[i+1]);
            nodeinfo.mess.id = 0;
        }
        if (strcasecmp(ptr[i], "Alias") == 0) {
            if (vhost->host) {
                *errtype = TYPESYNTAX;
                return SMULALB;
            }
            vhost->host = ptr[i+1];
        }
        if (strcasecmp(ptr[i], "Context") == 0) {
            if (vhost->context) {
                *errtype = TYPESYNTAX;
                return SMULCTB;
            }
            vhost->context = ptr[i+1];
        }
        i++;
        i++;
    }

    /* Check for JVMRoute, Alias and Context */
    if (nodeinfo.mess.JVMRoute[0] == '\0') {
        *errtype = TYPESYNTAX;
        return SROUBAD;
    }
    if (vhost->context == NULL && vhost->host != NULL) {
        *errtype = TYPESYNTAX;
        return SALIBAD;
    }
    if (vhost->host == NULL && vhost->context != NULL) {
        *errtype = TYPESYNTAX;
        return SCONBAD;
    }

    /* Read the node */
    node = read_node(nodestatsmem, &nodeinfo);
    if (node == NULL) {
        if (status == REMOVE)
            return NULL; /* Already done */
        *errtype = TYPEMEM;
        return MNODERD;
    }

    /* If the node is marked removed check what to do */
    if (node->mess.remove) {
        if (status == REMOVE)
            return NULL; /* Already done */
        else {
            /* Act has if the node wasn't found */
            *errtype = TYPEMEM;
            return MNODERD;
        }
    }

    /* Process the * APP commands */
    if (global) {
        return (process_node_cmd(r, status, errtype, node));
    }

    /* Read the ID of the virtual host corresponding to the first Alias */
    hostinfo.node = node->mess.id;
    if (vhost->host != NULL) {
        char *s = hostinfo.host;
        int j = 1;
        strncpy(hostinfo.host, vhost->host, sizeof(hostinfo.host));
        while (*s != ',' && j<sizeof(hostinfo.host)) {
           j++;
           s++;
        }
        *s = '\0';
    } else
        hostinfo.host[0] = '\0';

    hostinfo.id = 0;
    host = read_host(hoststatsmem, &hostinfo);
    if (host == NULL) {
        /* If REMOVE ignores it */
        if (status == REMOVE)
            return NULL;
        else {
            int vid, size, *id;
            /* Find the first available vhost id */
            vid = 0;
            size = loc_get_max_size_host();
            id = apr_palloc(r->pool, sizeof(int) * size);
            size = get_ids_used_host(hoststatsmem, id);
            for (i=0; i<size; i++) {
                hostinfo_t *ou;
                if (get_host(hoststatsmem, &ou, id[i]) != APR_SUCCESS)
                    continue;

	        if(ou->node == node->mess.id && ou->vhost > vid)
	            vid = ou->vhost;
            }
            vid++; /* Use next one. */
            ap_log_error(APLOG_MARK, APLOG_DEBUG, 0, r->server, "process_appl_cmd: adding vhost: %d node: %d",
                         vid, node->mess.id);

            /* If the Host doesn't exist yet create it */
            if (insert_update_hosts(hoststatsmem, vhost->host, node->mess.id, vid) != APR_SUCCESS) {
                *errtype = TYPEMEM;
                return MHOSTUI; 
            }
            hostinfo.id = 0;
            hostinfo.node = node->mess.id;
            if (vhost->host != NULL)
                strcpy(hostinfo.host, vhost->host);
            else
                hostinfo.host[0] = '\0';
            host = read_host(hoststatsmem, &hostinfo);
            if (host == NULL) {
                *errtype = TYPEMEM;
                return MHOSTRD; 
            }
        }
    }

    if (status == ENABLED) {
        /* There is no load balancing between balancers */
        int size = loc_get_max_size_context();
        int *id = apr_palloc(r->pool, sizeof(int) * size);
        size = get_ids_used_context(contextstatsmem, id);
        for (i=0; i<size; i++) {
            contextinfo_t *ou;
            if (get_context(contextstatsmem, &ou, id[i]) != APR_SUCCESS)
                continue;
            if (strcmp(ou->context, vhost->context) ==0) {
                /* There is the same context somewhere else */
                nodeinfo_t *hisnode;
                if (get_node(nodestatsmem, &hisnode, ou->node) != APR_SUCCESS)
                    continue;
                if (strcmp(hisnode->mess.balancer, node->mess.balancer)) {
                    /* the same context would be on 2 different balancer */
                    ap_log_error(APLOG_MARK, APLOG_NOERRNO|APLOG_WARNING, 0, r->server,
                                 "ENABLE: context %s is in balancer %s and %s", vhost->context,
                                  node->mess.balancer, hisnode->mess.balancer);
                }
            }
        }
    }

    /* Now update each context from Context: part */
    if (insert_update_contexts(contextstatsmem, vhost->context, node->mess.id, host->vhost, status) != APR_SUCCESS) {
        *errtype = TYPEMEM;
        return MCONTUI; 
    }

    /* Remove the host if all the contextes have been removed */
    if (status == REMOVE) {
        int size = loc_get_max_size_context();
        int *id = apr_palloc(r->pool, sizeof(int) * size);
        size = get_ids_used_context(contextstatsmem, id);
        for (i=0; i<size; i++) {
            contextinfo_t *ou;
            if (get_context(contextstatsmem, &ou, id[i]) != APR_SUCCESS)
                continue;
            if (ou->vhost == host->vhost &&
                ou->node == node->mess.id)
                break;
        }
        if (i==size) {
            int size = loc_get_max_size_host();
            int *id = apr_palloc(r->pool, sizeof(int) * size);
            size = get_ids_used_host(hoststatsmem, id);
            for (i=0; i<size; i++) {
                 hostinfo_t *ou;

                 if (get_host(hoststatsmem, &ou, id[i]) != APR_SUCCESS)
                     continue;
                 if(ou->vhost == host->vhost && ou->node == node->mess.id)
                     remove_host(hoststatsmem, ou);
            }
        }
    } else if (status == STOPPED) {
        /* insert_update_contexts in fact makes that vhost->context corresponds only to the first context... */
        contextinfo_t in;
        contextinfo_t *ou;
        in.id = 0;
        strncpy(in.context, vhost->context, sizeof(in.context));
        in.vhost = host->vhost;
        in.node = node->mess.id;
        ou = read_context(contextstatsmem, &in);
        if (ou != NULL) {
            ap_log_error(APLOG_MARK, APLOG_DEBUG, 0, r->server, "process_appl_cmd: STOP-APP nbrequests %d", ou->nbrequests);
            if (fromnode) {
                ap_set_content_type(r, "text/plain");
                ap_rprintf(r, "Type=STOP-APP-RSP&JvmRoute=%.*s&Alias=%.*s&Context=%.*s&Requests=%d",
                           (int) sizeof(nodeinfo.mess.JVMRoute), nodeinfo.mess.JVMRoute,
                           (int) sizeof(vhost->host), vhost->host,
                           (int) sizeof(vhost->context), vhost->context,
                           ou->nbrequests);
                ap_rprintf(r, "\n");
            }
        } else {
            ap_log_error(APLOG_MARK, APLOG_DEBUG, 0, r->server, "process_appl_cmd: STOP-APP can't read_context");
        }
    }
    return NULL;
}
static char * process_enable(request_rec *r, char **ptr, int *errtype, int global)
{
    return process_appl_cmd(r, ptr, ENABLED, errtype, global, 0);
}
static char * process_disable(request_rec *r, char **ptr, int *errtype, int global)
{
    return process_appl_cmd(r, ptr, DISABLED, errtype, global, 0);
}
static char * process_stop(request_rec *r, char **ptr, int *errtype, int global, int fromnode)
{
    return process_appl_cmd(r, ptr, STOPPED, errtype, global, fromnode);
}
static char * process_remove(request_rec *r, char **ptr, int *errtype, int global)
{
    return process_appl_cmd(r, ptr, REMOVE, errtype, global, 0);
}

/*
 * JGroups feature routines
 */
static char * process_addid(request_rec *r, char **ptr, int *errtype)
{
    jgroupsidinfo_t jgroupsid;
    int i = 0;
    jgroupsid.jgroupsid[0] = '\0';
    while (ptr[i]) {
        if (strcasecmp(ptr[i], "JGroupUuid") == 0) {
            if (strlen(ptr[i+1])>=sizeof(jgroupsid.jgroupsid)) {
                 *errtype = TYPESYNTAX;
                 return SJIDBIG;
            }
            strcpy(jgroupsid.jgroupsid, ptr[i+1]);
        }
        if (strcasecmp(ptr[i], "JGroupData") == 0) {
            if (strlen(ptr[i+1])>=sizeof(jgroupsid.data)) {
                 *errtype = TYPESYNTAX;
                 return SJDDBIG;
            }
            strcpy(jgroupsid.data, ptr[i+1]);
        }
        i++;
        i++;
    }
    if (jgroupsid.jgroupsid[0] == '\0') {
        *errtype = TYPESYNTAX;
        return SJIDBAD;
    }
    if (insert_update_jgroupsid(jgroupsidstatsmem, &jgroupsid) != APR_SUCCESS) {
        *errtype = TYPEMEM;
        return MJBIDUI;
    }

    return NULL;
}
static char * process_removeid(request_rec *r, char **ptr, int *errtype)
{
    jgroupsidinfo_t jgroupsid;
    int i = 0;
    jgroupsid.jgroupsid[0] = '\0';
    while (ptr[i]) {
        if (strcasecmp(ptr[i], "JGroupUuid") == 0) {
            if (strlen(ptr[i+1])>=sizeof(jgroupsid.jgroupsid)) {
                 *errtype = TYPESYNTAX;
                 return SJIDBIG;
            }
            strcpy(jgroupsid.jgroupsid, ptr[i+1]);
        }
        i++;
        i++;
    }
    if (jgroupsid.jgroupsid[0] == '\0') {
        *errtype = TYPESYNTAX;
        return SJIDBAD;
    }
    remove_jgroupsid(jgroupsidstatsmem, &jgroupsid);
    return NULL;
}
/*
 * Query out should be something like:
 * JGroup: [9],JGroupUuid: node4, JGroupData: jgroupdata4
 * JGroup: [11],JGroupUuid: node6, JGroupData: jgroupdata6
 */
static void print_jgroupsid(request_rec *r, int id, jgroupsidinfo_t *jgroupsid)
{
    ap_rprintf(r, "JGroup: [%d],JGroupUuid: %.*s,JGroupData: %.*s\n",
               id,
               (int) sizeof(jgroupsid->jgroupsid), jgroupsid->jgroupsid,
               (int) sizeof(jgroupsid->data), jgroupsid->data);
}
static char * process_query(request_rec *r, char **ptr, int *errtype)
{
    jgroupsidinfo_t jgroupsid;
    int i = 0;
    jgroupsid.jgroupsid[0] = '\0';
    while (ptr[i]) {
        if (strcasecmp(ptr[i], "JGroupUuid") == 0) {
            if (strlen(ptr[i+1])>=sizeof(jgroupsid.jgroupsid)) {
                 *errtype = TYPESYNTAX;
                 return SJIDBIG;
            }
            strcpy(jgroupsid.jgroupsid, ptr[i+1]);
        }
        i++;
        i++;
    }
    if (jgroupsid.jgroupsid[0] == '\0') {
        jgroupsid.jgroupsid[0] = '*';
        jgroupsid.jgroupsid[1] = '\0';
    }
    if (strcmp(jgroupsid.jgroupsid, "*") == 0) {
        int size, i;
        int *id;
        size = loc_get_max_size_jgroupsid();
        if (size == 0)
            return NULL;
        id = apr_palloc(r->pool, sizeof(int) * size);
        size = get_ids_used_jgroupsid(jgroupsidstatsmem, id);
        for (i=0; i<size; i++) {
             jgroupsidinfo_t *ou;
             if (get_jgroupsid(jgroupsidstatsmem, &ou, id[i]) != APR_SUCCESS)
                 continue;
             print_jgroupsid(r, id[i], ou);
        }
    } else {
        jgroupsidinfo_t *ou;
        ou = read_jgroupsid(jgroupsidstatsmem, &jgroupsid);
        if (ou == NULL) {
           *errtype = TYPEMEM;
           return MJBIDRD;
        } else
            print_jgroupsid(r, ou->id, ou);
    }
    return NULL;
}

/*
 * Call the ping/pong logic
 * Do a ping/png request to the node and set the load factor.
 */
static int isnode_up(request_rec *r, int id, int Load)
{
    if (balancerhandler != NULL) {
        return (balancerhandler->proxy_node_isup(r, id, Load));
    }
    return OK;
}
/*
 * Call the ping/pong logic using scheme://host:port
 * Do a ping/png request to the node and set the load factor.
 */
static int ishost_up(request_rec *r, char *scheme, char *host, char *port)
{
    if (balancerhandler != NULL) {
        return (balancerhandler->proxy_host_isup(r, scheme, host, port));
    }
    return OK;
}
/*
 * Process the STATUS command
 * Load -1 : Broken
 * Load 0  : Standby.
 * Load 1-100 : Load factor.
 */
static char * process_status(request_rec *r, char **ptr, int *errtype)
{
    int Load = -1;
    nodeinfo_t nodeinfo;
    nodeinfo_t *node;

    int i = 0;

    ap_log_error(APLOG_MARK, APLOG_DEBUG, 0, r->server, "Processing STATUS");
    while (ptr[i]) {
        if (strcasecmp(ptr[i], "JVMRoute") == 0) {
            if (strlen(ptr[i+1])>=sizeof(nodeinfo.mess.JVMRoute)) {
                *errtype = TYPESYNTAX;
                return SROUBIG;
            }
            strcpy(nodeinfo.mess.JVMRoute, ptr[i+1]);
            nodeinfo.mess.id = 0;
        }
        else if (strcasecmp(ptr[i], "Load") == 0) {
            Load = atoi(ptr[i+1]);
        }
        else {
            *errtype = TYPESYNTAX;
            return apr_psprintf(r->pool, SBADFLD, ptr[i]);
        }
        i++;
        i++;
    }

    /* Read the node */
    node = read_node(nodestatsmem, &nodeinfo);
    if (node == NULL) {
        *errtype = TYPEMEM;
        return MNODERD;
    }

    /*
     * If the node is usualable do a ping/pong to prevent Split-Brain Syndrome
     * and update the worker status and load factor acccording to the test result.
     */
    ap_set_content_type(r, "text/plain");
    ap_rprintf(r, "Type=STATUS-RSP&JVMRoute=%.*s", (int) sizeof(nodeinfo.mess.JVMRoute), nodeinfo.mess.JVMRoute);

    if (isnode_up(r, node->mess.id, Load) != OK)
        ap_rprintf(r, "&State=NOTOK");
    else
        ap_rprintf(r, "&State=OK");
#if AP_MODULE_MAGIC_AT_LEAST(20101223,1)
    ap_rprintf(r, "&id=%d", (int) ap_scoreboard_image->global->restart_time);
#else
    if (ap_my_generation)
        ap_rprintf(r, "&id=%d", ap_my_generation);
    else
        ap_rprintf(r, "&id=%d", (int) ap_scoreboard_image->global->restart_time);
#endif

    ap_rprintf(r, "\n");
    return NULL;
}

/*
 * Process the PING command
 * With a JVMRoute does a cping/cpong in the node.
 * Without just answers ok.
 * NOTE: It is hard to cping/cpong a host + port but CONFIG + PING + REMOVE_APP *
 *       would do the same.
 */
static char * process_ping(request_rec *r, char **ptr, int *errtype)
{
    nodeinfo_t nodeinfo;
    nodeinfo_t *node;
    char *scheme = NULL;
    char *host = NULL;
    char *port = NULL;

    int i = 0;

    ap_log_error(APLOG_MARK, APLOG_DEBUG, 0, r->server, "Processing PING");
    nodeinfo.mess.id = -1;
    while (ptr[i] && ptr[i][0] != '\0') {
        if (strcasecmp(ptr[i], "JVMRoute") == 0) {
            if (strlen(ptr[i+1])>=sizeof(nodeinfo.mess.JVMRoute)) {
                *errtype = TYPESYNTAX;
                return SROUBIG;
            }
            strcpy(nodeinfo.mess.JVMRoute, ptr[i+1]);
            nodeinfo.mess.id = 0;
        }
        else if (strcasecmp(ptr[i], "Scheme") == 0)
            scheme = apr_pstrdup(r->pool, ptr[i+1]);
        else if (strcasecmp(ptr[i], "Host") == 0)
            host = apr_pstrdup(r->pool, ptr[i+1]);
        else if (strcasecmp(ptr[i], "Port") == 0)
            port = apr_pstrdup(r->pool, ptr[i+1]);
        else {
            *errtype = TYPESYNTAX;
            return apr_psprintf(r->pool, SBADFLD, ptr[i]);
        }
        i++;
        i++;
    }
    if (nodeinfo.mess.id == -1) {
        /* PING scheme, host, port or just httpd */
        if (scheme == NULL && host == NULL && port == NULL) {
            ap_set_content_type(r, "text/plain");
            ap_rprintf(r, "Type=PING-RSP&State=OK");
        }  else {
            if (scheme == NULL || host == NULL || port == NULL) {
                *errtype = TYPESYNTAX;
                return apr_psprintf(r->pool, SMISFLD);
            }
            ap_set_content_type(r, "text/plain");
            ap_rprintf(r, "Type=PING-RSP");

            if (ishost_up(r, scheme, host, port) != OK)
                ap_rprintf(r, "&State=NOTOK");
            else
                ap_rprintf(r, "&State=OK");
        }
    } else {

        /* Read the node */
        node = read_node(nodestatsmem, &nodeinfo);
        if (node == NULL) {
            *errtype = TYPEMEM;
            return MNODERD;
        }

        /*
         * If the node is usualable do a ping/pong to prevent Split-Brain Syndrome
         * and update the worker status and load factor acccording to the test result.
         */
        ap_set_content_type(r, "text/plain");
        ap_rprintf(r, "Type=PING-RSP&JVMRoute=%.*s", (int) sizeof(nodeinfo.mess.JVMRoute), nodeinfo.mess.JVMRoute);

        if (isnode_up(r, node->mess.id, -2) != OK)
            ap_rprintf(r, "&State=NOTOK");
        else
            ap_rprintf(r, "&State=OK");
    }
#if AP_MODULE_MAGIC_AT_LEAST(20101223,1)
    ap_rprintf(r, "&id=%d", (int) ap_scoreboard_image->global->restart_time);
#else
    if (ap_my_generation)
        ap_rprintf(r, "&id=%d", ap_my_generation);
    else
        ap_rprintf(r, "&id=%d", (int) ap_scoreboard_image->global->restart_time);
#endif

    ap_rprintf(r, "\n");
    return NULL;
}

/*
 * Decodes a '%' escaped string, and returns the number of characters
 * (From mod_proxy_ftp.c).
 */

/* already called in the knowledge that the characters are hex digits */
/* Copied from modules/proxy/proxy_util.c */
static int mod_manager_hex2c(const char *x)
{
    int i, ch;

#if !APR_CHARSET_EBCDIC
    ch = x[0];
    if (apr_isdigit(ch)) {
        i = ch - '0';
    }
    else if (apr_isupper(ch)) {
        i = ch - ('A' - 10);
    }
    else {
        i = ch - ('a' - 10);
    }
    i <<= 4;

    ch = x[1];
    if (apr_isdigit(ch)) {
        i += ch - '0';
    }
    else if (apr_isupper(ch)) {
        i += ch - ('A' - 10);
    }
    else {
        i += ch - ('a' - 10);
    }
    return i;
#else /*APR_CHARSET_EBCDIC*/
    /*
     * we assume that the hex value refers to an ASCII character
     * so convert to EBCDIC so that it makes sense locally;
     *
     * example:
     *
     * client specifies %20 in URL to refer to a space char;
     * at this point we're called with EBCDIC "20"; after turning
     * EBCDIC "20" into binary 0x20, we then need to assume that 0x20
     * represents an ASCII char and convert 0x20 to EBCDIC, yielding
     * 0x40
     */
    char buf[1];

    if (1 == sscanf(x, "%2x", &i)) {
        buf[0] = i & 0xFF;
        ap_xlate_proto_from_ascii(buf, 1);
        return buf[0];
    }
    else {
        return 0;
    }
#endif /*APR_CHARSET_EBCDIC*/
}
static int decodeenc(char *x)
{
    int i, j, ch;

    if (x[0] == '\0')
        return 0;               /* special case for no characters */
    for (i = 0, j = 0; x[i] != '\0'; i++, j++) {
        /* decode it if not already done */
        ch = x[i];
        if (ch == '%' && apr_isxdigit(x[i + 1]) && apr_isxdigit(x[i + 2])) {
            ch = mod_manager_hex2c(&x[i + 1]);
            i += 2;
        }
        x[j] = ch;
    }
    x[j] = '\0';
    return j;
}

/* Check that the method is one of ours */
static int check_method(request_rec *r)
{
    int ours = 0;
    if (strcasecmp(r->method, "CONFIG") == 0)
        ours = 1;
    else if (strcasecmp(r->method, "ENABLE-APP") == 0)
        ours = 1;
    else if (strcasecmp(r->method, "DISABLE-APP") == 0)
        ours = 1;
    else if (strcasecmp(r->method, "STOP-APP") == 0)
        ours = 1;
    else if (strcasecmp(r->method, "REMOVE-APP") == 0)
        ours = 1;
    else if (strcasecmp(r->method, "STATUS") == 0)
        ours = 1;
    else if (strcasecmp(r->method, "DUMP") == 0)
        ours = 1;
    else if (strcasecmp(r->method, "ERROR") == 0)
        ours = 1;
    else if (strcasecmp(r->method, "INFO") == 0)
        ours = 1;
    else if (strcasecmp(r->method, "PING") == 0)
        ours = 1;
    else if (strcasecmp(r->method, "ADDID") == 0)
        ours = 1;
    else if (strcasecmp(r->method, "REMOVEID") == 0)
        ours = 1;
    else if (strcasecmp(r->method, "QUERY") == 0)
        ours = 1;
    return ours;
}
/*
 * This routine is called before mod_proxy translate name.
 * This allows us to make decisions before mod_proxy
 * to be able to fill tables even with ProxyPass / balancer...
 */
static int manager_trans(request_rec *r)
{
    int ours = 0;
    core_dir_config *conf =
        (core_dir_config *)ap_get_module_config(r->per_dir_config,
                                                &core_module);
    mod_manager_config *mconf = ap_get_module_config(r->server->module_config,
                                                     &manager_module);
 
    if (conf && conf->handler && r->method_number == M_GET &&
        strcmp(conf->handler, "mod_cluster-manager") == 0) {
        r->handler = "mod_cluster-manager";
        r->filename = apr_pstrdup(r->pool, r->uri);
        return OK;
    }
    if (r->method_number != M_INVALID)
        return DECLINED;
    if (!mconf->enable_mcpm_receive)
        return DECLINED; /* Not allowed to receive MCMP */

    ours = check_method(r); 
    if (ours) {
        int i;
        /* The method one of ours */
        ap_log_error(APLOG_MARK, APLOG_DEBUG, 0, r->server,
                    "manager_trans %s (%s)", r->method, r->uri);
        r->handler = "mod-cluster"; /* that hack doesn't work on httpd-2.4.x */
        i = strlen(r->uri);
        if (strcmp(r->uri, "*") == 0 || (i>=2 && r->uri[i-1] == '*' && r->uri[i-2] == '/')) {
            r->filename = apr_pstrdup(r->pool, NODE_COMMAND);
        } else {
            r->filename = apr_pstrdup(r->pool, r->uri);
        }
        return OK;
    }
    
    return DECLINED;
}

/* Create the commands that are possible on the context */
static char*context_string(request_rec *r, contextinfo_t *ou, char *Alias, char *JVMRoute)
{
    char context[sizeof(ou->context)+1];
    char *raw;
    context[sizeof(ou->context)] = '\0';
    strncpy(context, ou->context, sizeof(ou->context));
    raw = apr_pstrcat(r->pool, "JVMRoute=", JVMRoute, "&Alias=", Alias, "&Context=", context, NULL);
    return raw;
}
static char *balancer_nonce_string(request_rec *r)
{
    char *ret = "";
    void *sconf = r->server->module_config;
    mod_manager_config *mconf = ap_get_module_config(sconf, &manager_module);
    if (mconf->nonce)
        ret = apr_psprintf(r->pool, "nonce=%s&", balancer_nonce);
    return ret;
}
static void context_command_string(request_rec *r, contextinfo_t *ou, char *Alias, char *JVMRoute)
{
    if (ou->status == DISABLED) {
        ap_rprintf(r, "<a class=\"btn btn-success btn-xs mc-context-enable\" href=\"%s?%sCmd=ENABLE-APP&Range=CONTEXT&%s\">Enable</a> ",
                   r->uri, balancer_nonce_string(r), context_string(r, ou, Alias, JVMRoute));
        ap_rprintf(r, " <a class=\"btn btn-danger btn-xs mc-context-stop\" href=\"%s?%sCmd=STOP-APP&Range=CONTEXT&%s\">Stop</a>",
                   r->uri, balancer_nonce_string(r), context_string(r, ou, Alias, JVMRoute));
    }
    if (ou->status == ENABLED) {
        ap_rprintf(r, "<a class=\"btn btn-warning btn-xs mc-context-disable\" href=\"%s?%sCmd=DISABLE-APP&Range=CONTEXT&%s\">Disable</a>",
                   r->uri, balancer_nonce_string(r), context_string(r, ou, Alias, JVMRoute));
        ap_rprintf(r, " <a class=\"btn btn-danger btn-xs mc-context-stop\" href=\"%s?%sCmd=STOP-APP&Range=CONTEXT&%s\">Stop</a>",
                   r->uri, balancer_nonce_string(r), context_string(r, ou, Alias, JVMRoute));
    }
    if (ou->status == STOPPED) {
        ap_rprintf(r, "<a class=\"btn btn-success btn-xs mc-context-enable\" href=\"%s?%sCmd=ENABLE-APP&Range=CONTEXT&%s\">Enable</a> ",
                   r->uri, balancer_nonce_string(r), context_string(r, ou, Alias, JVMRoute));
        ap_rprintf(r, "<a class=\"btn btn-warning btn-xs mc-context-disable\" href=\"%s?%sCmd=DISABLE-APP&Range=CONTEXT&%s\">Disable</a>",
                   r->uri, balancer_nonce_string(r), context_string(r, ou, Alias, JVMRoute));
    }
}
/* Create the commands that are possible on the node */
static char*node_string(request_rec *r, char *JVMRoute)
{
    char *raw = apr_pstrcat(r->pool, "JVMRoute=", JVMRoute, NULL);
    return raw;
}
static void node_command_string(request_rec *r, char *JVMRoute)
{
    ap_rprintf(r, "<a class=\"btn btn-success btn-xs mc-node-enable\" href=\"%s?%sCmd=ENABLE-APP&Range=NODE&%s\">Enable Contexts</a> ",
               r->uri, balancer_nonce_string(r), node_string(r, JVMRoute));
    ap_rprintf(r, "<a class=\"btn btn-warning btn-xs mc-node-disable\" href=\"%s?%sCmd=DISABLE-APP&Range=NODE&%s\">Disable Contexts</a> ",
               r->uri, balancer_nonce_string(r), node_string(r, JVMRoute));
    ap_rprintf(r, "<a class=\"btn btn-danger btn-xs mc-node-stop\" href=\"%s?%sCmd=STOP-APP&Range=NODE&%s\">Stop Contexts</a>",
               r->uri, balancer_nonce_string(r), node_string(r, JVMRoute));
}
static void domain_command_string(request_rec *r, char *Domain)
{
    ap_rprintf(r, "<a class=\"btn btn-success btn-xs mc-domain-enable\" href=\"%s?%sCmd=ENABLE-APP&Range=DOMAIN&Domain=%s\">Enable Nodes</a> ",
               r->uri, balancer_nonce_string(r), Domain);
    ap_rprintf(r, "<a class=\"btn btn-warning btn-xs mc-domain-warning\" href=\"%s?%sCmd=DISABLE-APP&Range=DOMAIN&Domain=%s\">Disable Nodes</a> ",
               r->uri, balancer_nonce_string(r), Domain);
    ap_rprintf(r, "<a class=\"btn btn-danger btn-xs mc-domain-danger\" href=\"%s?%sCmd=STOP-APP&Range=DOMAIN&Domain=%s\">Stop Nodes</a>",
               r->uri, balancer_nonce_string(r), Domain);
}

/*
 * Process the parameters and display corresponding informations.
 */
static void manager_info_contexts(request_rec *r, int reduce_display, int allow_cmd, int node, int host, char *Alias, char *JVMRoute)
{
    int size, i;
    int *id;
    /* Process the Contexts */
    ap_rprintf(r, "<table class=\"table table-hover\"><thead><tr><th>Context</th><th>Status</th><th>Requests</th><th>Actions</th></tr></thead><tbody>");
    size = loc_get_max_size_context();
    if (size == 0)
        return;
    id = apr_palloc(r->pool, sizeof(int) * size);
    size = get_ids_used_context(contextstatsmem, id);
    for (i=0; i<size; i++) {
        contextinfo_t *ou;
        char *status;
        if (get_context(contextstatsmem, &ou, id[i]) != APR_SUCCESS)
            continue;
        if (ou->node != node || ou->vhost != host)
            continue;
        status = "REMOVED";
        switch (ou->status) {
            case ENABLED:
                status = "ENABLED";
                break;
            case DISABLED:
                status = "DISABLED";
                break;
            case STOPPED:
                status = "STOPPED";
                break;
        }
        ap_rprintf(r, "<tr><td>%.*s</td><td>%s</td><td>%d</td><td>", (int) sizeof(ou->context), ou->context, status, ou->nbrequests);
        if (allow_cmd)
            context_command_string(r, ou, Alias, JVMRoute);
        ap_rprintf(r, "</td></tr>\n");
    }
    ap_rprintf(r, "</tbody></table>");
}
static void manager_info_hosts(request_rec *r, int reduce_display, int allow_cmd, int node, char *JVMRoute)
{
    int size, i, j;
    int *id, *idChecker;
    int vhost = 0;

    /* Process the Vhosts */
    size = loc_get_max_size_host();
    if (size == 0)
        return;
    id = apr_palloc(r->pool, sizeof(int) * size);
    size = get_ids_used_host(hoststatsmem, id);
    idChecker = apr_pcalloc(r->pool, sizeof(int) * size);
    for (i=0; i<size; i++) {
        hostinfo_t *ou;
        if (get_host(hoststatsmem, &ou, id[i]) != APR_SUCCESS)
            continue;
        if (ou->node != node)
            continue;
        if (ou->vhost != vhost) {
            /* if we've logged this already, contine */
            if (idChecker[i] == 1)
                continue;
                ap_rprintf(r, "<h3 class=\"mc-vhost-title\"> Virtual Host <span class=\"mc-vhost-name\">%d</span>:</h3>", ou->vhost);
            manager_info_contexts(r, reduce_display, allow_cmd, ou->node, ou->vhost, ou->host, JVMRoute);
                ap_rprintf(r, "<h3>Aliases:</h3>");
                ap_rprintf(r, "<pre>");
            vhost = ou->vhost;
        
                ap_rprintf(r, "%.*s\n", (int) sizeof(ou->host), ou->host);
            
            /* Go ahead and check for any other later alias entries for this vhost and print them now */
            for (j=i+1; j<size; j++) {
                hostinfo_t *pv;
                if (get_host(hoststatsmem, &pv, id[j]) != APR_SUCCESS)
                    continue;
                if (pv->node != node)
                    continue;
                if (pv->vhost != vhost)
                    continue;

                /* mark this entry as logged */
                idChecker[j]=1;
                /* step the outer loop forward if we can */
                if (i == j-1)
                    i++;
                ap_rprintf(r, "%.*s\n", (int) sizeof(pv->host), pv->host);
            }
            ap_rprintf(r, "</pre>");
        }
    }
     

}
static void manager_sessionid(request_rec *r)
{
    int size, i;
    int *id;

    /* Process the Sessionids */
    size = loc_get_max_size_sessionid();
    if (size == 0)
        return;
    id = apr_palloc(r->pool, sizeof(int) * size);
    size = get_ids_used_sessionid(sessionidstatsmem, id);
    if (!size)
        return;
    ap_rprintf(r, "<h1>SessionIDs:</h1>");
    ap_rprintf(r, "<pre>");
    for (i=0; i<size; i++) {
        sessionidinfo_t *ou;
        if (get_sessionid(sessionidstatsmem, &ou, id[i]) != APR_SUCCESS)
            continue;
        ap_rprintf(r, "id: %.*s route: %.*s\n", (int) sizeof(ou->sessionid), ou->sessionid, (int) sizeof(ou->JVMRoute), ou->JVMRoute);
    }
    ap_rprintf(r, "</pre>");

}

#if HAVE_CLUSTER_EX_DEBUG
static void manager_domain(request_rec *r, int reduce_display)
{
    int size, i;
    int *id;

    /* Process the domain information: the removed node belonging to a domain are stored there */
    ap_rprintf(r, "<h1>LBGroup:</h1>");
    ap_rprintf(r, "<pre>");
    size = loc_get_max_size_domain();
    if (size == 0)
        return;
    id = apr_palloc(r->pool, sizeof(int) * size);
    size = get_ids_used_domain(domainstatsmem, id);
    for (i=0; i<size; i++) {
        domaininfo_t *ou;
        if (get_domain(domainstatsmem, &ou, id[i]) != APR_SUCCESS)
            continue;
        ap_rprintf(r, "dom: %.*s route: %.*s balancer: %.*s\n",
                   sizeof(ou->domain), ou->domain,
                   sizeof(ou->JVMRoute), ou->JVMRoute,
                   sizeof(ou->balancer), ou->balancer);
    }
    ap_rprintf(r, "</pre>");

}
#endif

static int count_sessionid(request_rec *r, char *route)
{
    int size, i;
    int *id;
    int count = 0;

    /* Count the sessionid corresponding to the route */
    size = loc_get_max_size_sessionid();
    if (size == 0)
        return 0;
    id = apr_palloc(r->pool, sizeof(int) * size);
    size = get_ids_used_sessionid(sessionidstatsmem, id);
    for (i=0; i<size; i++) {
        sessionidinfo_t *ou;
        if (get_sessionid(sessionidstatsmem, &ou, id[i]) != APR_SUCCESS)
            continue;
        if (strcmp(route, ou->JVMRoute) == 0)
            count++; 
    }
    return count;
}
static void process_error(request_rec *r, char *errstring, int errtype)
{
    r->status_line = apr_psprintf(r->pool, "ERROR");
    apr_table_setn(r->err_headers_out, "Version", VERSION_PROTOCOL);
    switch (errtype) {
      case TYPESYNTAX:
         apr_table_setn(r->err_headers_out, "Type", "SYNTAX");
         break;
      case TYPEMEM:
         apr_table_setn(r->err_headers_out, "Type", "MEM");
         break;
      default:
         apr_table_setn(r->err_headers_out, "Type", "GENERAL");
         break;
    }
    apr_table_setn(r->err_headers_out, "Mess", errstring);
    ap_log_error(APLOG_MARK, APLOG_NOERRNO|APLOG_WARNING, 0, r->server,
            "manager_handler %s error: %s", r->method, errstring);
}
static void sort_nodes(nodeinfo_t *nodes, int nbnodes)
{
    int i;
    int changed = -1;
    if (nbnodes <=1)
        return;
    while(changed) {
        changed = 0;
        for (i=0; i<nbnodes-1; i++) {
            if (strcmp(nodes[i].mess.Domain, nodes[i+1].mess.Domain)> 0) {
                nodeinfo_t node;
                node = nodes[i+1];
                nodes[i+1] = nodes[i];
                nodes[i] = node;
                changed = -1;
            }
        }
    }
}
static char *process_domain(request_rec *r, char **ptr, int *errtype, const char *cmd, const char *domain)
{
    int size, i;
    int *id;
    int pos;
    char *errstring = NULL;
    size = loc_get_max_size_node();
    if (size == 0)
        return NULL;
    id = apr_palloc(r->pool, sizeof(int) * size);
    size = get_ids_used_node(nodestatsmem, id);

    for (pos=0;ptr[pos]!=NULL && ptr[pos+1]!=NULL; pos=pos+2) ;

    ptr[pos] = apr_pstrdup(r->pool, "JVMRoute");
    ptr[pos+2] = NULL;
    ptr[pos+3] = NULL;
    ap_log_error(APLOG_MARK, APLOG_NOERRNO|APLOG_EMERG, 0, r->server, "process_domain");
    for (i=0; i<size; i++) {
        nodeinfo_t *ou;
        if (get_node(nodestatsmem, &ou, id[i]) != APR_SUCCESS)
            continue;
        if (strcmp(ou->mess.Domain, domain) != 0)
            continue;
        /* add the JVMRoute */
        ptr[pos+1] = apr_pstrdup(r->pool, ou->mess.JVMRoute);
        if (strcasecmp(cmd, "ENABLE-APP") == 0)
            errstring = process_enable(r, ptr, errtype, RANGENODE);
        else if (strcasecmp(cmd, "DISABLE-APP") == 0)
            errstring = process_disable(r, ptr, errtype, RANGENODE);
        else if (strcasecmp(cmd, "STOP-APP") == 0)
            errstring = process_stop(r, ptr, errtype, RANGENODE, 0);
        else if (strcasecmp(cmd, "REMOVE-APP") == 0)
            errstring = process_remove(r, ptr, errtype, RANGENODE);
    }
    return errstring;
}
/* XXX: move to mod_proxy_cluster as a provider ? */
#if AP_MODULE_MAGIC_AT_LEAST(20101223,1)
static void printproxy_stat(request_rec *r, int reduce_display, proxy_worker_shared *proxystat)
#else
static void printproxy_stat(request_rec *r, int reduce_display, proxy_worker_stat *proxystat)
#endif
{
    char *status = NULL;
    if (proxystat->status & PROXY_WORKER_NOT_USABLE_BITMAP)
        status = "NOTOK";
    else
        status = "OK";
    ap_rprintf(r, ",Status: %s,Elected: %d,Read: %d,Transferred: %d,Connected: %d,Load: %d",
           status,
           (int) proxystat->elected, (int) proxystat->read, (int) proxystat->transferred,
           (int) proxystat->busy, proxystat->lbfactor);
}
/* Display module information */
static void modules_info(request_rec *r)
{
    if (ap_find_linked_module("mod_proxy_cluster.c") != NULL)
        ap_rputs("mod_proxy_cluster.c: OK<br/>", r);
    else
        ap_rputs("mod_proxy_cluster.c: missing<br/>", r);

    if (ap_find_linked_module("mod_sharedmem.c") != NULL)
        ap_rputs("mod_sharedmem.c: OK<br/>", r);
    else
        ap_rputs("mod_sharedmem.c: missing<br/>", r);

    ap_rputs("Protocol supported: ", r);
    if (ap_find_linked_module("mod_proxy_http.c") != NULL)
        ap_rputs("http ", r);
    if (ap_find_linked_module("mod_proxy_ajp.c") != NULL) 
        ap_rputs("AJP ", r);
    if (ap_find_linked_module("mod_ssl.c") != NULL) 
        ap_rputs("https", r);
    ap_rputs("<br/>", r);

    if (ap_find_linked_module("mod_advertise.c") != NULL)
        ap_rputs("mod_advertise.c: OK<br/>", r);
    else
        ap_rputs("mod_advertise.c: not loaded<br/>", r);

}
/* Process INFO message and mod_cluster_manager pages generation */
static int manager_info(request_rec *r)
{
    int size, i, sizesessionid;
    int *id;
    apr_table_t *params = apr_table_make(r->pool, 10);
    int access_status;
    const char *name;
    nodeinfo_t *nodes;
    int nbnodes = 0;
    char *domain = "";
    char *errstring = NULL;
    void *sconf = r->server->module_config;
    mod_manager_config *mconf = ap_get_module_config(sconf, &manager_module);

    if (r->args) {
        char *args = apr_pstrdup(r->pool, r->args);
        char *tok, *val;
        while (args && *args) {
            if ((val = ap_strchr(args, '='))) {
                *val++ = '\0';
                if ((tok = ap_strchr(val, '&')))
                    *tok++ = '\0';
                /*
                 * Special case: contexts contain path information
                 */
                if ((access_status = ap_unescape_url(val)) != OK)
                    if (strcmp(args, "Context") || (access_status !=  HTTP_NOT_FOUND))
                        return access_status;
                apr_table_setn(params, args, val);
                args = tok;
            }
            else
                return HTTP_BAD_REQUEST;
        }
        ap_log_error(APLOG_MARK, APLOG_DEBUG, 0, r->server,
                "manager_info request:%s", r->args);
    }

    /*
     * Check that the supplied nonce matches this server's nonce;
     * otherwise ignore all parameters, to prevent a CSRF attack.
     */
    if (mconf->nonce && ((name = apr_table_get(params, "nonce")) == NULL
        || strcmp(balancer_nonce, name) != 0)) {
        apr_table_clear(params);
    }

    /* process the parameters */
    if (r->args) {
        const char *val = apr_table_get(params, "Refresh");
        const char *cmd = apr_table_get(params, "Cmd");
        const char *typ = apr_table_get(params, "Range");
        const char *domain = apr_table_get(params, "Domain");
        /* Process the Refresh parameter */
        if (val) {
            long t = atol(val);
            apr_table_set(r->headers_out, "Refresh", apr_ltoa(r->pool,t < 1 ? 10 : t));
        }
        /* Process INFO and DUMP */
        if (cmd != NULL) {
            int errtype = 0;
            if (strcasecmp(cmd, "DUMP") == 0) {
                errstring = process_dump(r, &errtype);
                if (!errstring)
                    return OK;
            } else if (strcasecmp(cmd, "INFO") == 0) {
                errstring = process_info(r, &errtype);
                if (!errstring)
                    return OK;
            }
            if (errstring) {
                process_error(r, errstring, errtype);
            }
        }
        /* Process other command if any */
        if (cmd != NULL && typ !=NULL && mconf->allow_cmd && errstring == NULL) {
            int global = RANGECONTEXT;
            int errtype = 0;
            int i;
            char **ptr;
            const apr_array_header_t *arr = apr_table_elts(params);
            const apr_table_entry_t *elts = (const apr_table_entry_t *)arr->elts;

            if (strcasecmp(typ,"NODE")==0)
                global = RANGENODE;
            else if (strcasecmp(typ,"DOMAIN")==0)
                global = RANGEDOMAIN;

            if (global == RANGEDOMAIN)
                ptr = apr_palloc(r->pool, sizeof(char *) * (arr->nelts + 2) * 2);
            else
                ptr = apr_palloc(r->pool, sizeof(char *) * (arr->nelts + 1) * 2);
            for (i = 0; i < arr->nelts; i++) {
                ptr[i*2] = elts[i].key;
                ptr[i*2+1] = elts[i].val;
            }
            ptr[arr->nelts*2] = NULL;
            ptr[arr->nelts*2+1] = NULL;
             
            if (global == RANGEDOMAIN)
                errstring = process_domain(r, ptr, &errtype, cmd, domain);
            else if (strcasecmp(cmd, "ENABLE-APP") == 0)
                errstring = process_enable(r, ptr, &errtype, global);
            else if (strcasecmp(cmd, "DISABLE-APP") == 0)
                errstring = process_disable(r, ptr, &errtype, global);
            else if (strcasecmp(cmd, "STOP-APP") == 0)
                errstring = process_stop(r, ptr, &errtype, global, 0);
            else if (strcasecmp(cmd, "REMOVE-APP") == 0)
                errstring = process_remove(r, ptr, &errtype, global);
            else {
                errstring = SCMDUNS;
                errtype = TYPESYNTAX;
            }
            if (errstring) {
                process_error(r, errstring, errtype);
            }
        }
    }
    
    ap_set_content_type(r, "text/html; charset=ISO-8859-1");
    ap_rvputs(r, "<!DOCTYPE html>\n\
<html lang=\"en\">\n\
                <head>\n \
                    <meta charset=\"utf-8\"/> \
                    <meta http-equiv=\"X-UA-Compatible\" content=\"IE=edge\"/> \
                    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1\"/> \
                    <style>", BOOTSTRAP3_CSS, "</style>\n \
                    <title>Mod_cluster Status</title>\n \
                </head> \
                <body>\
                <div class=\"container-fluid\" role=\"main\">\n \
                <div class=\"header\">\
                    <ul class=\"nav nav-pills pull-right\"> \
                        <li class=\"active\"><a href\"", r->uri , "\">Home</a></li>\
                        <li><a href=\"", r->uri, "?", balancer_nonce_string(r), "Cmd=DUMP&Range=ALL\">Dump</a></li>\
                        <li><a href=\"", r->uri, "?", balancer_nonce_string(r), "Cmd=INFO&Range=ALL\">Info</a></li>\
                        <li><a href=\"", r->uri, "?", balancer_nonce_string(r), "refresh=10\">Auto Refresh</a></li>\
                    </ul> \
                    <h3 class=\"text-muted\">", MOD_CLUSTER_EXPOSED_VERSION, "</h3>\
                </div><br/>\
                ", NULL);

    if (errstring) {
        ap_rvputs(r, "<h1 class=\"mc-command-failure\"> Command failed: ", errstring , "</h1>\n", NULL);
        ap_rvputs(r, " <a href=\"", r->uri, "\">Continue</a>\n", NULL);
        ap_rputs("</div></body></html>\n", r);
        return OK;
    }

    /* Advertise information */
    if (mconf->allow_display) {
        ap_rputs("<div class=\"panel panel-primary\"> \
                        <div class=\"panel-heading\">Allow Display</div>\
                        <div class=\"panel-body\">", r); 
        modules_info(r);
        if (advertise_info != NULL)
            advertise_info(r);
        ap_rputs("</div></div>", r);
    }


    sizesessionid = loc_get_max_size_sessionid();

    size = loc_get_max_size_node();
    if (size == 0)
        return OK;
    id = apr_palloc(r->pool, sizeof(int) * size);
    size = get_ids_used_node(nodestatsmem, id);


    /* read the node to sort them by domain */
    nodes = apr_palloc(r->pool, sizeof(nodeinfo_t) * size);
    for (i=0; i<size; i++) {
        nodeinfo_t *ou;
        if (get_node(nodestatsmem, &ou, id[i]) != APR_SUCCESS)
            continue;
        memcpy(&nodes[nbnodes],ou, sizeof(nodeinfo_t));
        nbnodes++;
    }
    sort_nodes(nodes, nbnodes);

    /* display the ordered nodes */
    for (i=0; i<size; i++) {
        char *flushpackets;
        nodeinfo_t *ou = &nodes[i];
        char *pptr = (char *) ou;

        if (strcmp(domain, ou->mess.Domain) != 0) {

            /* that means that we are not in our first itteration and domain was set */
            if ( strlen(domain) > 0  ) {
                ap_rprintf(r, "</div></div>\n");
            }
            ap_rprintf(r, "<div class=\"panel panel-primary\"><div class=\"panel-heading\">LBGroup %.*s ", (int) sizeof(ou->mess.Domain), ou->mess.Domain);
            domain = ou->mess.Domain;
            if (mconf->allow_cmd) {
                ap_rprintf(r, "<div class=\"pull-right\">");
                domain_command_string(r, domain);
                ap_rprintf(r, "</div>");
            }
            ap_rprintf(r, "</div><div class=\"panel-body\">");
        }


        ap_rprintf(r, "<div class=\"panel panel-primary\"><div class=\"panel-heading\">Node %.*s (%.*s://%.*s:%.*s)",
               (int) sizeof(ou->mess.JVMRoute), ou->mess.JVMRoute,
               (int) sizeof(ou->mess.Type), ou->mess.Type,
               (int) sizeof(ou->mess.Host), ou->mess.Host,
               (int) sizeof(ou->mess.Port), ou->mess.Port);
        if (mconf->allow_cmd) {
            ap_rprintf(r, "<div class=\"pull-right\">");
            node_command_string(r, ou->mess.JVMRoute);
            ap_rprintf(r, "</div>");
        }

        ap_rputs("</div> <div class=\"panel-body\">", r);

        pptr = pptr + ou->offset;

        ap_rprintf(r, "Balancer: %.*s,LBGroup: %.*s", (int) sizeof(ou->mess.balancer), ou->mess.balancer,
               (int) sizeof(ou->mess.Domain), ou->mess.Domain);

        flushpackets = "Off";
        switch (ou->mess.flushpackets) {
            case flush_on:
                flushpackets = "On";
                break;
            case flush_auto:
                flushpackets = "Auto";
        }
        ap_rprintf(r, ",Flushpackets: %s,Flushwait: %d,Ping: %d,Smax: %d,Ttl: %d",
               flushpackets, ou->mess.flushwait,
               (int) ou->mess.ping, ou->mess.smax, (int) ou->mess.ttl);

#if AP_MODULE_MAGIC_AT_LEAST(20101223,1)
        printproxy_stat(r, mconf->reduce_display, (proxy_worker_shared *) pptr);
#else
        printproxy_stat(r, mconf->reduce_display, (proxy_worker_stat *) pptr);
#endif

        if (sizesessionid) {
            ap_rprintf(r, ",Num sessions: %d",  count_sessionid(r, ou->mess.JVMRoute));
        }

        /* Process the Vhosts */
        manager_info_hosts(r, mconf->reduce_display, mconf->allow_cmd, ou->mess.id, ou->mess.JVMRoute); 
        ap_rprintf(r, "</div></div>");
    }
    /* Display the sessions */
    if (sizesessionid)
        manager_sessionid(r);
#if HAVE_CLUSTER_EX_DEBUG
    manager_domain(r, mconf->reduce_display);
#endif

            /* we are on the last iterration of lb group */
            if ( strlen(domain) > 0 &&  i == size  ) {
                ap_rprintf(r, "</div></div>\n");
            }

    ap_rputs("</div></body></html>\n", r);
    return OK;
}

/* Process the requests from the ModClusterService */
static int manager_handler(request_rec *r)
{
    apr_bucket_brigade *input_brigade;
    char *errstring = NULL;
    int errtype = 0;
    char *buff;
    apr_size_t bufsiz=0, maxbufsiz, len;
    apr_status_t status;
    int global = 0;
    int ours = 0;
    char **ptr;
    void *sconf = r->server->module_config;
    mod_manager_config *mconf;
  
    if (strcmp(r->handler, "mod_cluster-manager") == 0) {
        /* Display the nodes information */
        if (r->method_number != M_GET)
            return DECLINED;
        return(manager_info(r));
    }

    mconf = ap_get_module_config(sconf, &manager_module);
    if (!mconf->enable_mcpm_receive)
        return DECLINED; /* Not allowed to receive MCMP */

    ours = check_method(r);
    if (!ours)
        return DECLINED;

    /* Use a buffer to read the message */
    if (mconf->maxmesssize)
       maxbufsiz = mconf->maxmesssize;
    else {
       /* we calculate it */
       maxbufsiz = 9 + JVMROUTESZ;
       maxbufsiz = bufsiz + (mconf->maxhost * HOSTALIASZ) + 7;
       maxbufsiz = bufsiz + (mconf->maxcontext * CONTEXTSZ) + 8;
    }
    if (maxbufsiz< MAXMESSSIZE)
       maxbufsiz = MAXMESSSIZE;
    buff = apr_pcalloc(r->pool, maxbufsiz);
    input_brigade = apr_brigade_create(r->pool, r->connection->bucket_alloc);
    len = maxbufsiz;
    while ((status = ap_get_brigade(r->input_filters, input_brigade, AP_MODE_READBYTES, APR_BLOCK_READ, len)) == APR_SUCCESS) {
        apr_brigade_flatten(input_brigade, buff + bufsiz, &len);
        apr_brigade_cleanup(input_brigade);
        bufsiz += len;
        if (bufsiz >= maxbufsiz || len == 0) break;
        len = maxbufsiz - bufsiz;
    }

    if (status != APR_SUCCESS) {
        errstring = apr_psprintf(r->pool, SREADER, r->method);
        r->status_line = apr_psprintf(r->pool, "ERROR");
        apr_table_setn(r->err_headers_out, "Version", VERSION_PROTOCOL);
        apr_table_setn(r->err_headers_out, "Type", "SYNTAX");
        apr_table_setn(r->err_headers_out, "Mess", errstring);
        ap_log_error(APLOG_MARK, APLOG_DEBUG, 0, r->server,
                "manager_handler %s error: %s", r->method, errstring);
        return 500;
    }
    buff[bufsiz] = '\0';

    /* XXX: Size limit it? */
    ap_log_error(APLOG_MARK, APLOG_DEBUG, 0, r->server,
                "manager_handler %s (%s) processing: \"%s\"", r->method, r->filename, buff);

    decodeenc(buff);
    ptr = process_buff(r, buff);
    if (ptr == NULL) {
        process_error(r, SMESPAR, TYPESYNTAX);
        return 500;
    }
    if (strstr(r->filename, NODE_COMMAND))
        global = 1;

    if (strcasecmp(r->method, "CONFIG") == 0)
        errstring = process_config(r, ptr, &errtype);
    /* Application handling */
    else if (strcasecmp(r->method, "ENABLE-APP") == 0)
        errstring = process_enable(r, ptr, &errtype, global);
    else if (strcasecmp(r->method, "DISABLE-APP") == 0)
        errstring = process_disable(r, ptr, &errtype, global);
    else if (strcasecmp(r->method, "STOP-APP") == 0)
        errstring = process_stop(r, ptr, &errtype, global, 1);
    else if (strcasecmp(r->method, "REMOVE-APP") == 0)
        errstring = process_remove(r, ptr, &errtype, global);
    /* Status handling */
    else if (strcasecmp(r->method, "STATUS") == 0)
        errstring = process_status(r, ptr, &errtype);
    else if (strcasecmp(r->method, "DUMP") == 0)
        errstring = process_dump(r, &errtype);
    else if (strcasecmp(r->method, "INFO") == 0)
        errstring = process_info(r, &errtype);
    else if (strcasecmp(r->method, "PING") == 0)
        errstring = process_ping(r, ptr, &errtype);
    else if (strcasecmp(r->method, "ADDID") == 0)
        errstring = process_addid(r, ptr, &errtype);
    else if (strcasecmp(r->method, "REMOVEID") == 0)
        errstring = process_removeid(r, ptr, &errtype);
    else if (strcasecmp(r->method, "QUERY") == 0)
        errstring = process_query(r, ptr, &errtype);
    else {
        errstring = SCMDUNS;
        errtype = TYPESYNTAX;
    }

    /* Check error string and build the error message */
    if (errstring) {
        process_error(r, errstring, errtype);
        return 500;
    }

    ap_log_error(APLOG_MARK, APLOG_DEBUG, 0, r->server,
                "manager_handler %s  OK", r->method);

    ap_rflush(r);
    return (OK);
}

/*
 *  Attach to the shared memory when the child is created.
 */
static void  manager_child_init(apr_pool_t *p, server_rec *s)
{
    char *node;
    char *context;
    char *host;
    char *balancer;
    char *sessionid;
    mod_manager_config *mconf = ap_get_module_config(s->module_config, &manager_module);

    if (storage == NULL) {
        /* that happens when doing a gracefull restart for example after additing/changing the storage provider */
        ap_log_error(APLOG_MARK, APLOG_NOERRNO|APLOG_EMERG, 0, s, "Fatal storage provider not initialized");
        return;
    }

    mconf->tableversion = 0;

    if (mconf->basefilename) {
        node = apr_pstrcat(p, mconf->basefilename, "/manager.node", NULL);
        context = apr_pstrcat(p, mconf->basefilename, "/manager.context", NULL);
        host = apr_pstrcat(p, mconf->basefilename, "/manager.host", NULL);
        balancer = apr_pstrcat(p, mconf->basefilename, "/manager.balancer", NULL);
        sessionid = apr_pstrcat(p, mconf->basefilename, "/manager.sessionid", NULL);
    } else {
        node = ap_server_root_relative(p, "logs/manager.node");
        context = ap_server_root_relative(p, "logs/manager.context");
        host = ap_server_root_relative(p, "logs/manager.host");
        balancer = ap_server_root_relative(p, "logs/manager.balancer");
        sessionid = ap_server_root_relative(p, "logs/manager.sessionid");
    }

    nodestatsmem = get_mem_node(node, &mconf->maxnode, p, storage);
    if (nodestatsmem == NULL) {
        ap_log_error(APLOG_MARK, APLOG_NOERRNO|APLOG_EMERG, 0, s, "get_mem_node %s failed", node);
        return;
    }
    if (get_last_mem_error(nodestatsmem) != APR_SUCCESS) {
        char buf[120];
        ap_log_error(APLOG_MARK, APLOG_NOERRNO|APLOG_EMERG, 0, s, "get_mem_node %s failed: %s",
                     node, apr_strerror(get_last_mem_error(nodestatsmem), buf, sizeof(buf)));
        return;
    }

    contextstatsmem = get_mem_context(context, &mconf->maxcontext, p, storage);
    if (contextstatsmem == NULL) {
        ap_log_error(APLOG_MARK, APLOG_NOERRNO|APLOG_EMERG, 0, s, "get_mem_context failed");
        return;
    }

    hoststatsmem = get_mem_host(host, &mconf->maxhost, p, storage);
    if (hoststatsmem == NULL) {
        ap_log_error(APLOG_MARK, APLOG_NOERRNO|APLOG_EMERG, 0, s, "get_mem_host failed");
        return;
    }

    balancerstatsmem = get_mem_balancer(balancer, &mconf->maxhost, p, storage);
    if (balancerstatsmem == NULL) {
        ap_log_error(APLOG_MARK, APLOG_NOERRNO|APLOG_EMERG, 0, s, "get_mem_balancer failed");
        return;
    }

    if (mconf->maxsessionid) {
        /*  Try to get sessionid stuff only if required */
        sessionidstatsmem = get_mem_sessionid(sessionid, &mconf->maxsessionid, p, storage);
        if (sessionidstatsmem == NULL) {
            ap_log_error(APLOG_MARK, APLOG_NOERRNO|APLOG_EMERG, 0, s, "get_mem_sessionid failed");
            return;
        }
    }
}

/*
 * Supported directives.
 */
static const char *cmd_manager_maxcontext(cmd_parms *cmd, void *mconfig, const char *word)
{
    mod_manager_config *mconf = ap_get_module_config(cmd->server->module_config, &manager_module);
    const char *err = ap_check_cmd_context(cmd, GLOBAL_ONLY);
    if (err != NULL) {
        return err;
    }
    mconf->maxcontext = atoi(word);
    return NULL;
}
static const char *cmd_manager_maxnode(cmd_parms *cmd, void *mconfig, const char *word)
{
    mod_manager_config *mconf = ap_get_module_config(cmd->server->module_config, &manager_module);
    const char *err = ap_check_cmd_context(cmd, GLOBAL_ONLY);
    if (err != NULL) {
        return err;
    }
    mconf->maxnode = atoi(word);
    return NULL;
}
static const char *cmd_manager_maxhost(cmd_parms *cmd, void *mconfig, const char *word)
{
    mod_manager_config *mconf = ap_get_module_config(cmd->server->module_config, &manager_module);
    const char *err = ap_check_cmd_context(cmd, GLOBAL_ONLY);
    if (err != NULL) {
        return err;
    }
    mconf->maxhost = atoi(word);
    return NULL;
}
static const char *cmd_manager_maxsessionid(cmd_parms *cmd, void *mconfig, const char *word)
{
    mod_manager_config *mconf = ap_get_module_config(cmd->server->module_config, &manager_module);
    const char *err = ap_check_cmd_context(cmd, GLOBAL_ONLY);
    if (err != NULL) {
        return err;
    }
    mconf->maxsessionid = atoi(word);
    return NULL;
}
static const char *cmd_manager_maxjgroupsid(cmd_parms *cmd, void *mconfig, const char *word)
{
    mod_manager_config *mconf = ap_get_module_config(cmd->server->module_config, &manager_module);
    const char *err = ap_check_cmd_context(cmd, GLOBAL_ONLY);
    if (err != NULL) {
        return err;
    }
    mconf->maxjgroupsid = atoi(word);
    return NULL;
}
static const char *cmd_manager_memmanagerfile(cmd_parms *cmd, void *mconfig, const char *word)
{
    mod_manager_config *mconf = ap_get_module_config(cmd->server->module_config, &manager_module);
    const char *err = ap_check_cmd_context(cmd, GLOBAL_ONLY);
    if (err != NULL) {
        return err;
    }
    mconf->basefilename = apr_pstrdup(cmd->pool, word);
    if (apr_dir_make_recursive(mconf->basefilename, APR_UREAD | APR_UWRITE | APR_UEXECUTE, cmd->pool) != APR_SUCCESS)
        return  "Can't create directory corresponding to MemManagerFile";
    return NULL;
}
static const char *cmd_manager_balancername(cmd_parms *cmd, void *mconfig, const char *word)
{
    mod_manager_config *mconf = ap_get_module_config(cmd->server->module_config, &manager_module);
    mconf->balancername = apr_pstrdup(cmd->pool, word);
    return NULL;
}
static const char*cmd_manager_pers(cmd_parms *cmd, void *dummy, const char *arg)
{
    mod_manager_config *mconf = ap_get_module_config(cmd->server->module_config, &manager_module);
    const char *err = ap_check_cmd_context(cmd, GLOBAL_ONLY);
    if (err != NULL) {
        return err;
    }
    if (strcasecmp(arg, "Off") == 0)
       mconf->persistent = 0;
    else if (strcasecmp(arg, "On") == 0)
       mconf->persistent = CREPER_SLOTMEM;
    else {
       return "PersistSlots must be one of: "
              "off | on";
    }
    return NULL;
}

static const char*cmd_manager_nonce(cmd_parms *cmd, void *dummy, const char *arg)
{
    mod_manager_config *mconf = ap_get_module_config(cmd->server->module_config, &manager_module);
    if (strcasecmp(arg, "Off") == 0)
       mconf->nonce = 0;
    else if (strcasecmp(arg, "On") == 0)
       mconf->nonce = -1;
    else {
       return "CheckNonce must be one of: "
              "off | on";
    }
    return NULL;
}
static const char*cmd_manager_allow_display(cmd_parms *cmd, void *dummy, const char *arg)
{
    mod_manager_config *mconf = ap_get_module_config(cmd->server->module_config, &manager_module);
    if (strcasecmp(arg, "Off") == 0)
       mconf->allow_display = 0;
    else if (strcasecmp(arg, "On") == 0)
       mconf->allow_display = -1;
    else {
       return "AllowDisplay must be one of: "
              "off | on";
    }
    return NULL;
}
static const char*cmd_manager_allow_cmd(cmd_parms *cmd, void *dummy, const char *arg)
{
    mod_manager_config *mconf = ap_get_module_config(cmd->server->module_config, &manager_module);
    if (strcasecmp(arg, "Off") == 0)
       mconf->allow_cmd = 0;
    else if (strcasecmp(arg, "On") == 0)
       mconf->allow_cmd = -1;
    else {
       return "AllowCmd must be one of: "
              "off | on";
    }
    return NULL;
}
static const char*cmd_manager_reduce_display(cmd_parms *cmd, void *dummy, const char *arg)
{
    mod_manager_config *mconf = ap_get_module_config(cmd->server->module_config, &manager_module);
    if (strcasecmp(arg, "Off") == 0)
       mconf->reduce_display = 0;
    else if (strcasecmp(arg, "On") == 0)
       mconf->reduce_display = -1;
    else {
       return "ReduceDisplay must be one of: "
              "off | on";
    }
    return NULL;
}
static const char*cmd_manager_maxmesssize(cmd_parms *cmd, void *mconfig, const char *word)
{
    mod_manager_config *mconf = ap_get_module_config(cmd->server->module_config, &manager_module);
    const char *err = ap_check_cmd_context(cmd, GLOBAL_ONLY);
    if (err != NULL) {
        return err;
    }
    mconf->maxmesssize = atoi(word);
    if (mconf->maxmesssize < MAXMESSSIZE)
       return "MaxMCMPMessSize must bigger than 1024";
    return NULL;
}
static const char*cmd_manager_enable_mcpm_receive(cmd_parms *cmd, void *dummy)
{
    mod_manager_config *mconf = ap_get_module_config(cmd->server->module_config, &manager_module);
    if (!cmd->server->is_virtual)
        return "EnableMCPMReceive must be in a VirtualHost";
    mconf->enable_mcpm_receive = -1;
    return NULL;
}


static const command_rec  manager_cmds[] =
{
    AP_INIT_TAKE1(
        "Maxcontext",
        cmd_manager_maxcontext,
        NULL,
        OR_ALL,
        "Maxcontext - number max context supported by mod_cluster"
    ),
    AP_INIT_TAKE1(
        "Maxnode",
        cmd_manager_maxnode,
        NULL,
        OR_ALL,
        "Maxnode - number max node supported by mod_cluster"
    ),
    AP_INIT_TAKE1(
        "Maxhost",
        cmd_manager_maxhost,
        NULL,
        OR_ALL,
        "Maxhost - number max host (Alias in virtual hosts) supported by mod_cluster"
    ),
    AP_INIT_TAKE1(
        "Maxsessionid",
        cmd_manager_maxsessionid,
        NULL,
        OR_ALL,
        "Maxsessionid - number session (Used to track number of sessions per nodes) supported by mod_cluster"
    ),
    AP_INIT_TAKE1(
        "Maxjgroupsid",
        cmd_manager_maxjgroupsid,
        NULL,
        OR_ALL,
        "Maxjgroupsid - number jgroupsid supported by mod_cluster"
    ),
    AP_INIT_TAKE1(
        "MemManagerFile",
        cmd_manager_memmanagerfile,
        NULL,
        OR_ALL,
        "MemManagerFile - base name of the files used to create/attach to shared memory"
    ),
    AP_INIT_TAKE1(
        "ManagerBalancerName",
        cmd_manager_balancername,
        NULL,
        OR_ALL,
        "ManagerBalancerName - name of a balancer corresponding to the manager"
    ),
    AP_INIT_TAKE1(
        "PersistSlots",
        cmd_manager_pers,
        NULL,
        OR_ALL,
        "PersistSlots - Persist the slot mem elements on | off (Default: off No persistence)"
    ),
    AP_INIT_TAKE1(
        "CheckNonce",
        cmd_manager_nonce,
        NULL,
        OR_ALL,
        "CheckNonce - Switch check of nonce when using mod_cluster-manager handler on | off (Default: on Nonce checked)"
    ),
    AP_INIT_TAKE1(
        "AllowDisplay",
        cmd_manager_allow_display,
        NULL,
        OR_ALL,
        "AllowDisplay - Display additional information in the mod_cluster-manager page on | off (Default: off Only version displayed)"
    ),
    AP_INIT_TAKE1(
        "AllowCmd",
        cmd_manager_allow_cmd,
        NULL,
        OR_ALL,
        "AllowCmd - Allow commands using mod_cluster-manager URL on | off (Default: on Commmands allowed)"
    ),
    AP_INIT_TAKE1(
        "ReduceDisplay",
        cmd_manager_reduce_display,
        NULL,
        OR_ALL,
        "ReduceDisplay - Don't contexts in the main mod_cluster-manager page. on | off (Default: off Context displayed)"
    ),
   AP_INIT_TAKE1(
        "MaxMCMPMessSize",
        cmd_manager_maxmesssize,
        NULL,
        OR_ALL,
        "MaxMCMPMaxMessSize - Maximum size of MCMP messages. (Default: calculated min value: 1024)"
    ),
    AP_INIT_NO_ARGS(
        "EnableMCPMReceive",
         cmd_manager_enable_mcpm_receive,
         NULL,
         OR_ALL,
         "EnableMCPMReceive - Allow the VirtualHost to receive MCPM."
    ),
    {NULL}
};

/* hooks declaration */

static void manager_hooks(apr_pool_t *p)
{
    static const char * const aszSucc[]={ "mod_proxy.c", NULL };

    /* Create the shared tables for mod_proxy_cluster */
    ap_hook_post_config(manager_init, NULL, NULL, APR_HOOK_MIDDLE);

    /* Attach to the shared tables with create the child */
    ap_hook_child_init(manager_child_init, NULL, NULL, APR_HOOK_MIDDLE);

    /* post read_request handling: to be handle to use ProxyPass / */
    ap_hook_translate_name(manager_trans, NULL, aszSucc,
                              APR_HOOK_FIRST);

    /* Process the request from the ModClusterService */
    ap_hook_handler(manager_handler, NULL, NULL, APR_HOOK_REALLY_FIRST);

    /* Register nodes/hosts/contexts table provider */
    ap_register_provider(p, "manager" , "shared", "0", &node_storage);
    ap_register_provider(p, "manager" , "shared", "1", &host_storage);
    ap_register_provider(p, "manager" , "shared", "2", &context_storage);
    ap_register_provider(p, "manager" , "shared", "3", &balancer_storage);
    ap_register_provider(p, "manager" , "shared", "4", &sessionid_storage);
    ap_register_provider(p, "manager" , "shared", "5", &domain_storage);
}

/*
 * Config creation stuff
 */
static void *create_manager_config(apr_pool_t *p)
{
    mod_manager_config *mconf = apr_pcalloc(p, sizeof(*mconf));

    mconf->basefilename = NULL;
    mconf->maxcontext = DEFMAXCONTEXT;
    mconf->maxnode = DEFMAXNODE;
    mconf->maxhost = DEFMAXHOST;
    mconf->maxsessionid = DEFMAXSESSIONID;
    mconf->maxjgroupsid = DEFMAXJGROUPSID;
    mconf->tableversion = 0;
    mconf->persistent = 0;
    mconf->nonce = -1;
    mconf->balancername = NULL;
    mconf->allow_display = 0;
    mconf->allow_cmd = -1;
    mconf->reduce_display = 0;
    mconf->enable_mcpm_receive = 0;
    return mconf;
}

static void *create_manager_server_config(apr_pool_t *p, server_rec *s)
{
    return(create_manager_config(p));
}
static void *merge_manager_server_config(apr_pool_t *p, void *server1_conf,
                                         void *server2_conf)
{
    mod_manager_config *mconf1 = (mod_manager_config *) server1_conf;
    mod_manager_config *mconf2 = (mod_manager_config *) server2_conf;
    mod_manager_config *mconf = apr_pcalloc(p, sizeof(*mconf));

    mconf->basefilename = NULL;
    mconf->maxcontext = DEFMAXCONTEXT;
    mconf->maxnode = DEFMAXNODE;
    mconf->tableversion = 0;
    mconf->persistent = 0;
    mconf->nonce = -1;
    mconf->balancername = NULL;
    mconf->allow_display = 0;
    mconf->allow_cmd = -1;
    mconf->reduce_display = 0;

    if (mconf2->basefilename)
        mconf->basefilename = apr_pstrdup(p, mconf2->basefilename);
    else if (mconf1->basefilename)
        mconf->basefilename = apr_pstrdup(p, mconf1->basefilename);

    if (mconf2->maxcontext != DEFMAXCONTEXT)
        mconf->maxcontext = mconf2->maxcontext;
    else if (mconf1->maxcontext != DEFMAXCONTEXT)
        mconf->maxcontext = mconf1->maxcontext;

    if (mconf2->maxnode != DEFMAXNODE)
        mconf->maxnode = mconf2->maxnode;
    else if (mconf1->maxnode != DEFMAXNODE)
        mconf->maxnode = mconf1->maxnode;

    if (mconf2->maxhost != DEFMAXHOST)
        mconf->maxhost = mconf2->maxhost;
    else if (mconf1->maxhost != DEFMAXHOST)
        mconf->maxhost = mconf1->maxhost;

    if (mconf2->maxsessionid != DEFMAXSESSIONID)
        mconf->maxsessionid = mconf2->maxsessionid;
    else if (mconf1->maxsessionid != DEFMAXSESSIONID)
        mconf->maxsessionid = mconf1->maxsessionid;

    if (mconf2->maxjgroupsid != DEFMAXJGROUPSID)
        mconf->maxjgroupsid = mconf2->maxjgroupsid;
    else if (mconf1->maxjgroupsid != DEFMAXJGROUPSID)
        mconf->maxjgroupsid = mconf1->maxjgroupsid;

    if (mconf2->persistent != 0)
        mconf->persistent = mconf2->persistent;
    else if (mconf1->persistent != 0)
        mconf->persistent = mconf1->persistent;

    if (mconf2->nonce != -1)
        mconf->nonce = mconf2->nonce;
    else if (mconf1->nonce != -1)
        mconf->nonce = mconf1->nonce;

    if (mconf2->balancername)
        mconf->balancername = apr_pstrdup(p, mconf2->balancername);
    else if (mconf1->balancername)
        mconf->balancername = apr_pstrdup(p, mconf1->balancername);

    if (mconf2->allow_display != 0)
        mconf->allow_display = mconf2->allow_display;
    else if (mconf1->allow_display != 0)
        mconf->allow_display = mconf1->allow_display;

    if (mconf2->allow_cmd != -1)
        mconf->allow_cmd = mconf2->allow_cmd;
    else if (mconf1->allow_cmd != -1)
        mconf->allow_cmd = mconf1->allow_cmd;

    if (mconf2->reduce_display != 0)
        mconf->reduce_display = mconf2->reduce_display;
    else if (mconf1->reduce_display != 0)
        mconf->reduce_display = mconf1->reduce_display;

    if (mconf2->enable_mcpm_receive != 0)
        mconf->enable_mcpm_receive = mconf2->enable_mcpm_receive;
    else if (mconf1->enable_mcpm_receive != 0)
        mconf->enable_mcpm_receive = mconf1->enable_mcpm_receive;

    return mconf;
}

/* Module declaration */

module AP_MODULE_DECLARE_DATA manager_module = {
    STANDARD20_MODULE_STUFF,
    NULL,
    NULL,
    create_manager_server_config,
    merge_manager_server_config,
    manager_cmds,      /* command table */
    manager_hooks      /* register hooks */
};
