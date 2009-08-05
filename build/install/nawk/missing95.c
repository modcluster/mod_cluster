/* popen and pclose are not part of win 95 and nt,
   but it appears that _popen and _pclose "work".
   if this won't load, use the return NULL statements. */

#include <stdio.h>
#include <string.h>

FILE *popen(char *s, char *m)
{
    return _popen(s, m);    /* return NULL; */
}

int pclose(FILE *f)
{
    return _pclose(f);  /* return NULL; */
}

size_t strlcpy(char *dst, const char *src, size_t dst_sz)
{
    size_t n;

    for (n = 0; n < dst_sz; n++) {
        if ((*dst++ = *src++) != '\0')
            break;
    }
    if (n < dst_sz)
        return n;
    if (n > 0)
        *(dst -1) = '\0';

    return n + strlen(src);
}


size_t strlcat(char *dst, const char *src, size_t dst_sz)
{
    size_t len = strlen(dst);

    if (dst_sz < len)
        return len + strlen(src);

    return len + strlcpy(dst + len, src, dst_sz - len);
}
