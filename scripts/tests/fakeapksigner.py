"""A fake apksigner for testing Test-Release.ps1 without Java or a real APK.
FAKE_SIGNER_MODE: good | other | two | reject | broken. FAKE_SIGNER_DIGEST is the digest 'good' and 'two' print.
Nothing reads the APK argument."""
import os, sys

mode = os.environ.get("FAKE_SIGNER_MODE", "good")
digest = os.environ.get("FAKE_SIGNER_DIGEST", "0" * 64)
if mode == "good":
    print("Signer #1 certificate DN: CN=Test")
    print("Signer #1 certificate SHA-256 digest: " + digest)
elif mode == "other":
    print("Signer #1 certificate SHA-256 digest: " + "0" * 64)
elif mode == "two":
    print("Signer #1 certificate SHA-256 digest: " + digest)
    print("Signer #2 certificate SHA-256 digest: " + "1" * 64)
elif mode == "reject":
    sys.stderr.write("DOES NOT VERIFY\n")
    sys.exit(1)
elif mode == "broken":
    print("ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH.")
    sys.exit(1)
else:
    sys.exit(9)
