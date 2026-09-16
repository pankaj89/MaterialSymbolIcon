package com.master.materialsymbol.util

import org.junit.Assert.assertEquals
import org.junit.Test

class NamingHelperTest {

    @Test
    fun testToXmlDrawableName() {
        assertEquals("ic_shopping_cart", NamingHelper.toXmlDrawableName("shopping_cart"))
        assertEquals("ic_shopping_cart", NamingHelper.toXmlDrawableName("shopping-cart"))
        assertEquals("ic_shopping_cart", NamingHelper.toXmlDrawableName("shopping cart"))
        assertEquals("ic_shopping_cart", NamingHelper.toXmlDrawableName("ic_shopping_cart"))
        assertEquals("ic_arrow_up_and_down", NamingHelper.toXmlDrawableName("arrow.up.and.down"))
        assertEquals("ic_123_test", NamingHelper.toXmlDrawableName("123_test"))
        assertEquals("custom_home", NamingHelper.toXmlDrawableName("home", prefix = "custom_"))
    }

    @Test
    fun testToComposeName() {
        assertEquals("ShoppingCart", NamingHelper.toComposeName("shopping_cart"))
        assertEquals("ShoppingCart", NamingHelper.toComposeName("shopping-cart"))
        assertEquals("ShoppingCart", NamingHelper.toComposeName("ic_shopping_cart"))
        assertEquals("ArrowUpAndDown", NamingHelper.toComposeName("arrow.up.and.down"))
        assertEquals("ShoppingCartVector", NamingHelper.toComposeName("shopping_cart", suffix = "Vector"))
        assertEquals("Icon123Test", NamingHelper.toComposeName("123_test"))
    }

    @Test
    fun testExtractNameFromUrl() {
        val googleKtUrl = "https://fonts.gstatic.com/render/v1/Material+Symbols+Outlined/24dp/home.kt?var=opsz,wght,FILL,GRAD,ROND@24,400,0,0,50"
        assertEquals("home", NamingHelper.extractNameFromUrl(googleKtUrl))

        val googleSvgUrl = "https://fonts.gstatic.com/s/i/short-term/release/materialsymbolsoutlined/favorite/default/24px.svg"
        assertEquals("favorite", NamingHelper.extractNameFromUrl(googleSvgUrl))

        val standardSvgUrl = "https://example.com/assets/search-icon.svg"
        assertEquals("search_icon", NamingHelper.extractNameFromUrl(standardSvgUrl))
    }
}
