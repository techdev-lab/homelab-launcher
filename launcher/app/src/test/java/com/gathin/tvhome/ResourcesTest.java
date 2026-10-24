package com.gathin.tvhome;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * The name lists that moved out of the code into res/values and res/values-ar: both languages must have the
 * full list in the right length, or the Hijri date and the prayer card come out empty or shifted.
 */
public class ResourcesTest {

    private static List<String> array(String folder, String name) throws Exception {
        File f = new File("src/main/res/" + folder + "/strings.xml");
        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(f);
        NodeList arrays = doc.getElementsByTagName("string-array");
        List<String> out = new ArrayList<>();
        for (int i = 0; i < arrays.getLength(); i++) {
            Element a = (Element) arrays.item(i);
            if (!name.equals(a.getAttribute("name"))) continue;
            NodeList items = a.getElementsByTagName("item");
            for (int j = 0; j < items.getLength(); j++) out.add(items.item(j).getTextContent().trim());
        }
        return out;
    }

    private static void check(String name, int size) throws Exception {
        for (String folder : new String[]{"values", "values-ar"}) {
            List<String> items = array(folder, name);
            assertEquals(folder + "/" + name, size, items.size());
            for (String s : items) assertFalse(folder + "/" + name + " has an empty item", s.isEmpty());
        }
    }

    @Test
    public void prayerNames() throws Exception {
        check("prayer_names", 6);
    }

    @Test
    public void hijriMonths() throws Exception {
        check("hijri_months", 12);
    }

    @Test
    public void hijriDays() throws Exception {
        check("hijri_days", 7);
    }

    /** The names of the plain strings in a folder; strings marked translatable="false" are skipped on request. */
    private static Set<String> stringNames(String folder, boolean translatableOnly) throws Exception {
        File f = new File("src/main/res/" + folder + "/strings.xml");
        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(f);
        NodeList strings = doc.getElementsByTagName("string");
        Set<String> out = new TreeSet<>();
        for (int i = 0; i < strings.getLength(); i++) {
            Element s = (Element) strings.item(i);
            if (translatableOnly && "false".equals(s.getAttribute("translatable"))) continue;
            out.add(s.getAttribute("name"));
        }
        return out;
    }

    @Test
    public void everyTranslatableStringHasAnArabicVersion() throws Exception {
        Set<String> missing = new TreeSet<>(stringNames("values", true));
        missing.removeAll(stringNames("values-ar", false));
        assertTrue("values-ar is missing: " + missing, missing.isEmpty());
    }

    @Test
    public void missingStringIsCaught() throws Exception {
        assertFalse(stringNames("values-ar", false).contains("no_such_string"));
    }

    @Test
    public void missingArrayIsCaught() throws Exception {
        assertEquals(0, array("values", "no_such_array").size());
    }
}
