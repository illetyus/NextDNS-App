"""Reject empty test/coverage reports and stale screenshot fixtures in CI."""
from pathlib import Path
import struct
import xml.etree.ElementTree as ET
import zlib

root = Path(__file__).resolve().parents[1]
reports = list((root / "app/build/test-results/testDebugUnitTest").glob("TEST-*.xml"))
assert reports, "No executed unit-test reports found"
tests = failures = skipped = 0
cases = set()
for path in reports:
    suite = ET.parse(path).getroot()
    tests += int(suite.get("tests", 0))
    failures += int(suite.get("failures", 0)) + int(suite.get("errors", 0))
    skipped += int(suite.get("skipped", 0))
    cases.update((case.get("classname"), case.get("name")) for case in suite.findall("testcase"))
assert tests > 0 and failures == 0 and skipped == 0, (tests, failures, skipped)
for name in ("greeting_screenshot", "homeTitleFitsCompactDisplayWithLargeText"):
    assert ("com.example.GreetingScreenshotTest", name) in cases, f"Branding test was not executed: {name}"

for name in ("greeting.png", "greeting-compact-large-font.png"):
    path = root / "app/build/outputs/roborazzi/branding" / name
    data = path.read_bytes()
    assert data[:8] == b"\x89PNG\r\n\x1a\n", f"Invalid generated PNG: {path}"
    width, height = struct.unpack(">II", data[16:24])
    assert width >= 300 and height >= 300, f"Incomplete screen capture: {width}x{height}"
    offset = 8
    image_data = bytearray()
    while offset < len(data):
        length = int.from_bytes(data[offset:offset + 4], "big")
        kind = data[offset + 4:offset + 8]
        chunk = data[offset + 8:offset + 8 + length]
        checksum = int.from_bytes(data[offset + 8 + length:offset + 12 + length], "big")
        assert zlib.crc32(kind + chunk) == checksum, f"Corrupt PNG chunk: {path}"
        if kind == b"IDAT":
            image_data.extend(chunk)
        offset += 12 + length
    assert len(set(zlib.decompress(image_data))) > 8, f"Empty screen capture: {path}"

coverage = ET.parse(root / "app/build/reports/jacoco/jacocoTestReport/jacocoTestReport.xml").getroot()
classes = coverage.findall(".//class")
assert classes, "JaCoCo report contains no compiled application classes"
for class_name in ("LegalAcceptanceStore", "LegalDocuments"):
    class_path = "com/example/data/legal/" + class_name
    entry = next((item for item in classes if item.get("name") == class_path), None)
    assert entry is not None, f"Legal application class absent from coverage: {class_name}"
    lines = next((item for item in entry.findall("counter") if item.get("type") == "LINE"), None)
    assert lines is not None and int(lines.get("covered", 0)) > 0, f"Legal tests produced no executed lines: {class_name}"
for class_name in ("LegalAcceptanceStoreTest", "LegalDocumentsTest", "LegalDocumentLinksTest", "LegalWelcomeInteractionTest"):
    assert any(classname == "com.example.data.legal." + class_name for classname, name in cases), f"Legal test not executed: {class_name}"
instructions = next((item for item in coverage.findall("counter") if item.get("type") == "INSTRUCTION"), None)
assert instructions is not None and int(instructions.get("covered", 0)) > 0, "No executed application instructions in coverage"
print(f"PASS: {tests} tests, {failures} failures, {skipped} skipped; two generated screenshots; {len(classes)} coverage classes")
