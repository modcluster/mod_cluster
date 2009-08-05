#
# rewritesvc.awk script to rewrite the @VERSION@ tags in service.bat
#
BEGIN { 
    srcfl = ARGV[1];
    dstfl = ARGV[2];
    version = ARGV[3];
    major = ARGV[4];
    minor = ARGV[5];
    platform = ARGV[6];
    while ( ( getline < srcfl ) > 0 ) {
        gsub( /@VERSION@/, version );
        gsub( /@VERSION_MAJOR@/, major );
        gsub( /@VERSION_MINOR@/, minor );
        gsub( /@VERSION_PLATFORM@/, platform );
        print $0 > dstfl;
    }
}
