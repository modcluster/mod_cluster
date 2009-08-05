/* Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/* Utility routines for Apache proxy (addon for mod_cluster) */
#include "mod_proxy.h"

/**
 * Parse a given timeout parameter string into an apr_interval_time_t value.
 * The unit of the time interval is given as postfix string to the numeric
 * string. Currently the following units are understood:
 *
 * ms    : milliseconds
 * s     : seconds
 * mi[n] : minutes
 * h     : hours
 *
 * If no unit is contained in the given timeout parameter the default_time_unit
 * will be used instead.
 * @param timeout_parameter The string containing the timeout parameter.
 * @param timeout The timeout value to be returned.
 * @param default_time_unit The default time unit to use if none is specified
 * in timeout_parameter.
 * @return Status value indicating whether the parsing was successful or not.
 */
PROXY_DECLARE(apr_status_t) cluster_timeout_parameter_parse(
                                               const char *timeout_parameter,
                                               apr_interval_time_t *timeout,
                                               const char *default_time_unit)
{
    char *endp;
    const char *time_str;
    apr_int64_t tout;

    tout = apr_strtoi64(timeout_parameter, &endp, 10);
    if (errno) {
        return errno;
    }
    if (!endp || !*endp) {
        time_str = default_time_unit;
    }
    else {
        time_str = endp;
    }

    switch (*time_str) {
        /* Time is in seconds */
    case 's':
        *timeout = (apr_interval_time_t) apr_time_from_sec(tout);
        break;
    case 'h':
        /* Time is in hours */
        *timeout = (apr_interval_time_t) apr_time_from_sec(tout * 3600);
        break;
    case 'm':
        switch (*(++time_str)) {
        /* Time is in miliseconds */
        case 's':
            *timeout = (apr_interval_time_t) tout * 1000;
            break;
        /* Time is in minutes */
        case 'i':
            *timeout = (apr_interval_time_t) apr_time_from_sec(tout * 60);
            break;
        default:
            return APR_EGENERAL;
        }
        break;
    default:
        return APR_EGENERAL;
    }
    return APR_SUCCESS;
}

/* This one is in 2.2.8+ but not in 2.2.3 and 2.2.3 is the base for a some distribution (Suse) */
typedef struct hdr_ptr {
    ap_filter_t *f;
    apr_bucket_brigade *bb;
} hdr_ptr;
static int send_header(void *data, const char *key, const char *val)
{
    ap_fputstrs(((hdr_ptr*)data)->f, ((hdr_ptr*)data)->bb,
                key, ": ", val, CRLF, NULL);
    return 1;
}

PROXY_DECLARE(void) cluster_send_interim_response(request_rec *r, int send_headers)
{
    hdr_ptr x;
    char *status_line = NULL;

    if (r->proto_num < 1001) {
        /* don't send interim response to HTTP/1.0 Client */
        return;
    }
    if (!ap_is_HTTP_INFO(r->status)) {
        ap_log_rerror(APLOG_MARK, APLOG_DEBUG, 0, NULL,
                      "Status is %d - not sending interim response", r->status);
        return;
    }

    status_line = apr_pstrcat(r->pool, AP_SERVER_PROTOCOL, " ", r->status_line, CRLF, NULL);
    ap_xlate_proto_to_ascii(status_line, strlen(status_line));

    x.f = r->connection->output_filters;
    x.bb = apr_brigade_create(r->pool, r->connection->bucket_alloc);

    ap_fputs(x.f, x.bb, status_line);
    if (send_headers) {
        apr_table_do(send_header, &x, r->headers_out, NULL);
        apr_table_clear(r->headers_out);
    }
    ap_fputs(x.f, x.bb, CRLF);
    ap_fflush(x.f, x.bb);
    apr_brigade_destroy(x.bb);
}
