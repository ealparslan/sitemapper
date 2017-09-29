package com.sikayetvar.sitemap.generator;

import org.junit.Test;

public class URLTest {

    @Test
    public void testURLReplacements(){
        assert Utils.getInstance().toSlug("Den eme").equals("den-eme");
        assert Utils.getInstance().toSlug("Den<e>me").equals("deneme");
        assert Utils.getInstance().toSlug("D&e*n!e@m%e^+=").equals("deneme");
        assert Utils.getInstance().toSlug("çöşığü").equals("cosigu");
        assert Utils.getInstance().toSlug("ÇÖŞİĞÜ").equals("cosigu");
        assert Utils.getInstance().toSlug("--").equals("-");
        assert Utils.getInstance().toSlug("---").equals("-");
        assert Utils.getInstance().toSlug("----").equals("-");

    }
}
