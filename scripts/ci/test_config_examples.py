"""The example themes in config/ must be valid JSON, and their app_keys must be usable on the TV.

The launcher drops a protected key or a bad entry silently (AppKeys.java), so a typo in an example would just do nothing.
This test fails first. The protected key codes are the ones in AppKeys.PROTECTED: Home, Back, the arrows, OK, Power.
"""
import json
import pathlib
import unittest

ROOT = pathlib.Path(__file__).resolve().parents[2]
PROTECTED = {3, 4, 19, 20, 21, 22, 23, 26, 66, 160, 284}
EXAMPLES = sorted((ROOT / "config" / "examples").glob("*.json")) + [ROOT / "config" / "config.example.json"]


class ConfigExamplesTest(unittest.TestCase):
    def load(self, path):
        return json.loads(path.read_text(encoding="utf-8"))

    def test_every_example_is_valid_json(self):
        self.assertTrue(EXAMPLES)
        for path in EXAMPLES:
            with self.subTest(path=path.name):
                self.assertIsInstance(self.load(path), dict)

    def test_app_keys_are_usable(self):
        for path in EXAMPLES:
            keys = self.load(path).get("app_keys")
            if keys is None:
                continue
            with self.subTest(path=path.name):
                self.assertIsInstance(keys, dict)
                for code, app in keys.items():
                    self.assertTrue(code.isdigit() and int(code) > 0, f"bad key code {code!r}")
                    self.assertNotIn(int(code), PROTECTED, f"{code} is a protected key")
                    self.assertIsInstance(app, str)
                    self.assertTrue(app == "ignore" or "." in app, f"{app!r} is not a package name or 'ignore'")

    def test_the_saudi_examples_open_shahid_with_the_netflix_button(self):
        for name in ("riyadh.json", "offline-madinah.json"):
            with self.subTest(name=name):
                keys = self.load(ROOT / "config" / "examples" / name)["app_keys"]
                self.assertEqual("net.mbc.shahidTV", keys["191"])
                self.assertEqual("com.google.android.youtube.tv", keys["190"])


if __name__ == "__main__":
    unittest.main()
