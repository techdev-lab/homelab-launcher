import unittest

import check_hygiene

# Five Arabic letters, built from code points so this file contains no Arabic characters itself.
ARABIC_TEXT = "".join(chr(code) for code in (0x645, 0x631, 0x62D, 0x628, 0x627))


class ArabicViolationsTest(unittest.TestCase):
    def test_arabic_twin_docs_are_allowed(self):
        self.assertEqual(check_hygiene.arabic_violations("docs/JOURNEY.ar.md", ARABIC_TEXT), [])

    def test_localization_data_is_allowed(self):
        path = "launcher/app/src/main/res/values-ar/strings.xml"
        self.assertEqual(check_hygiene.arabic_violations(path, ARABIC_TEXT), [])
        self.assertEqual(check_hygiene.arabic_violations("config/examples/cairo.json", ARABIC_TEXT), [])

    def test_arabic_in_java_code_is_reported_with_its_line(self):
        text = "class A {\n    // " + ARABIC_TEXT + "\n}\n"
        path = "launcher/app/src/main/java/com/gathin/tvhome/A.java"
        self.assertEqual(check_hygiene.arabic_violations(path, text), [2])

    def test_arabic_in_english_docs_is_reported(self):
        self.assertEqual(check_hygiene.arabic_violations("docs/INSTALL.md", ARABIC_TEXT), [1])

    def test_bilingual_readme_is_allowed(self):
        text = f"# Title\n{ARABIC_TEXT}\n"
        self.assertEqual(check_hygiene.arabic_violations("README.md", text), [])

    def test_other_root_docs_are_still_reported(self):
        self.assertEqual(check_hygiene.arabic_violations("CONTRIBUTING.md", ARABIC_TEXT), [1])
        self.assertEqual(check_hygiene.arabic_violations("SECURITY.md", ARABIC_TEXT), [1])

    def test_english_text_is_clean(self):
        self.assertEqual(check_hygiene.arabic_violations("docs/INSTALL.md", "Plain English.\n"), [])


class ForbiddenFilesTest(unittest.TestCase):
    def test_secrets_and_local_files_are_forbidden(self):
        for path in ("launcher/local.properties", "keys/release.keystore", "a/b/adbkey", ".env", "x/debug.jks"):
            self.assertTrue(check_hygiene.is_forbidden(path), path)

    def test_normal_files_are_not_forbidden(self):
        for path in ("launcher/build.gradle", "config/config.example.json", "docs/env.md"):
            self.assertFalse(check_hygiene.is_forbidden(path), path)


if __name__ == "__main__":
    unittest.main()
