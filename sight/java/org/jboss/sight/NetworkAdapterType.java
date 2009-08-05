/*
 *  SIGHT - System information gathering hybrid tool
 *
 *  Copyright(c) 2007 Red Hat Middleware, LLC,
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
 */

package org.jboss.sight;

/**
 * IANA Network adapter types
 */
public enum NetworkAdapterType
{
    UNKNOWN(                 0),
    OTHER(                   1),   // None of the below
    REGULAR_1822(            2),
    HDH_1822(                3),
    DDN_X25(                 4),
    RFC877_X25(              5),
    ETHERNET_CSMACD(         6),
    IS088023_CSMACD(         7),
    ISO88024_TOKENBUS(       8),
    ISO88025_TOKENRING(      9),
    ISO88026_MAN(            10),
    STARLAN(                 11),
    PROTEON_10MBIT(          12),
    PROTEON_80MBIT(          13),
    HYPERCHANNEL(            14),
    FDDI(                    15),
    LAP_B(                   16),
    SDLC(                    17),
    DS1(                     18),  // DS1-MIB
    E1(                      19),  // Obsolete; see DS1-MIB
    BASIC_ISDN(              20),
    PRIMARY_ISDN(            21),
    PROP_POINT2POINT_SERIAL( 22),  // proprietary serial
    PPP(                     23),
    SOFTWARE_LOOPBACK(       24),
    EON(                     25),  // CLNP over IP
    ETHERNET_3MBIT(          26),
    NSIP(                    27),  // XNS over IP
    SLIP(                    28),  // Generic Slip
    ULTRA(                   29),  // ULTRA Technologies
    DS3(                     30),  // DS3-MIB
    SIP(                     31),  // SMDS, coffee
    FRAMERELAY(              32),  // DTE only
    RS232(                   33),
    PARA(                    34),  // Parallel port
    ARCNET(                  35),
    ARCNET_PLUS(             36),
    ATM(                     37),  // ATM cells
    MIO_X25(                 38),
    SONET(                   39),  // SONET or SDH
    X25_PLE(                 40),
    ISO88022_LLC(            41),
    LOCALTALK(               42),
    SMDS_DXI(                43),
    FRAMERELAY_SERVICE(      44),  // FRNETSERV-MIB
    V35(                     45),
    HSSI(                    46),
    HIPPI(                   47),
    MODEM(                   48),  // Generic Modem
    AAL5(                    49),  // AAL5 over ATM
    SONET_PATH(              50),
    SONET_VT(                51),
    SMDS_ICIP(               52),  // SMDS InterCarrier Interface
    PROP_VIRTUAL(            53),  // Proprietary virtual/internal
    PROP_MULTIPLEXOR(        54),  // Proprietary multiplexing
    IEEE80212(               55),  // 100BaseVG
    FIBRECHANNEL(            56),
    HIPPIINTERFACE(          57),
    FRAMERELAY_INTERCONNECT( 58),  // Obsolete, use 32 or 44
    AFLANE_8023(             59),  // ATM Emulated LAN for 802.3
    AFLANE_8025(             60),  // ATM Emulated LAN for 802.5
    CCTEMUL(                 61),  // ATM Emulated circuit
    FASTETHER(               62),  // Fast Ethernet (100BaseT)
    ISDN(                    63),  // ISDN and X.25
    V11(                     64),  // CCITT V.11/X.21
    V36(                     65),  // CCITT V.36
    G703_64K(                66),  // CCITT G703 at 64Kbps
    G703_2MB(                67),  // Obsolete; see DS1-MIB
    QLLC(                    68),  // SNA QLLC
    FASTETHER_FX(            69),  // Fast Ethernet (100BaseFX)
    CHANNEL(                 70),
    IEEE80211(               71),  // Radio spread spectrum
    IBM370PARCHAN(           72),  // IBM System 360/370 OEMI Channel
    ESCON(                   73),  // IBM Enterprise Systems Connection
    DLSW(                    74),  // Data Link Switching
    ISDN_S(                  75),  // ISDN S/T interface
    ISDN_U(                  76),  // ISDN U interface
    LAP_D(                   77),  // Link Access Protocol D
    IPSWITCH(                78),  // IP Switching Objects
    RSRB(                    79),  // Remote Source Route Bridging
    ATM_LOGICAL(             80),  // ATM Logical Port
    DS0(                     81),  // Digital Signal Level 0
    DS0_BUNDLE(              82),  // Group of ds0s on the same ds1
    BSC(                     83),  // Bisynchronous Protocol
    ASYNC(                   84),  // Asynchronous Protocol
    CNR(                     85),  // Combat Net Radio
    ISO88025R_DTR(           86),  // ISO 802.5r DTR
    EPLRS(                   87),  // Ext Pos Loc Report Sys
    ARAP(                    88),  // Appletalk Remote Access Protocol
    PROP_CNLS(               89),  // Proprietary Connectionless Proto
    HOSTPAD(                 90),  // CCITT-ITU X.29 PAD Protocol
    TERMPAD(                 91),  // CCITT-ITU X.3 PAD Facility
    FRAMERELAY_MPI(          92),  // Multiproto Interconnect over FR
    X213(                    93),  // CCITT-ITU X213
    ADSL(                    94),  // Asymmetric Digital Subscrbr Loop
    RADSL(                   95),  // Rate-Adapt Digital Subscrbr Loop
    SDSL(                    96),  // Symmetric Digital Subscriber Loop
    VDSL(                    97),  // Very H-Speed Digital Subscrb Loop
    ISO88025_CRFPRINT(       98),  // ISO 802.5 CRFP
    MYRINET(                 99),  // Myricom Myrinet
    VOICE_EM(                100), // Voice recEive and transMit
    VOICE_FXO(               101), // Voice Foreign Exchange Office
    VOICE_FXS(               102), // Voice Foreign Exchange Station
    VOICE_ENCAP(             103), // Voice encapsulation
    VOICE_OVERIP(            104), // Voice over IP encapsulation
    ATM_DXI(                 105), // ATM DXI
    ATM_FUNI(                106), // ATM FUNI
    ATM_IMA(                 107), // ATM IMA
    PPPMULTILINKBUNDLE(      108), // PPP Multilink Bundle
    IPOVER_CDLC(             109), // IBM ipOverCdlc
    IPOVER_CLAW(             110), // IBM Common Link Access to Workstn
    STACKTOSTACK(            111), // IBM stackToStack
    VIRTUALIPADDRESS(        112), // IBM VIPA
    MPC(                     113), // IBM multi-proto channel support
    IPOVER_ATM(              114), // IBM ipOverAtm
    ISO88025_FIBER(          115), // ISO 802.5j Fiber Token Ring
    TDLC(                    116), // IBM twinaxial data link control
    GIGABITETHERNET(         117),
    HDLC(                    118),
    LAP_F(                   119),
    V37(                     120),
    X25_MLP(                 121), // Multi-Link Protocol
    X25_HUNTGROUP(           122), // X.25 Hunt Group
    TRANSPHDLC(              123),
    INTERLEAVE(              124), // Interleave channel
    FAST(                    125), // Fast channel
    IP(                      126), // IP (for APPN HPR in IP networks)
    DOCSCABLE_MACLAYER(      127), // CATV Mac Layer
    DOCSCABLE_DOWNSTREAM(    128), // CATV Downstream interface
    DOCSCABLE_UPSTREAM(      129), // CATV Upstream interface
    A12MPPSWITCH(            130), // Avalon Parallel Processor
    TUNNEL(                  131), // Encapsulation interface
    COFFEE(                  132), // Coffee pot
    CES(                     133), // Circuit Emulation Service
    ATM_SUBINTERFACE(        134), // ATM Sub Interface
    L2_VLAN(                 135), // Layer 2 Virtual LAN using 802.1Q
    L3_IPVLAN(               136), // Layer 3 Virtual LAN using IP
    L3_IPXVLAN(              137), // Layer 3 Virtual LAN using IPX
    DIGITALPOWERLINE(        138), // IP over Power Lines
    MEDIAMAILOVERIP(         139), // Multimedia Mail over IP
    DTM(                     140), // Dynamic syncronous Transfer Mode
    DCN(                     141), // Data Communications Network
    IPFORWARD(               142), // IP Forwarding Interface
    MSDSL(                   143), // Multi-rate Symmetric DSL
    IEEE1394(                144), // IEEE1394 High Perf Serial Bus
    RECEIVE_ONLY(            145); // TV adapter type


    private int value;
    private NetworkAdapterType(int v)
    {
        value = v;
    }

    public int valueOf()
    {
        return value;
    }

    public static NetworkAdapterType valueOf(int value)
    {
        for (NetworkAdapterType e : values()) {
            if (e.value == value)
                return e;
        }
        return UNKNOWN;
    }

}
