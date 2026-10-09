import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.jar.JarFile;

/** Verify every data entry and the expected certificate without a public PKI claim. */
class VerifyBundleSigner {
  public static void main(String[] args) throws Exception {
    int count = 0;
    try (var jar = new JarFile(Path.of(args[0]).toFile(), true)) {
      var entries = jar.entries();
      while (entries.hasMoreElements()) {
        var entry = entries.nextElement();
        if (entry.isDirectory() || entry.getName().matches("(?i)META-INF/(MANIFEST\\.MF|[^/]+\\.(SF|RSA|DSA|EC))")) continue;
        try (var input = jar.getInputStream(entry)) { input.transferTo(java.io.OutputStream.nullOutputStream()); }
        var signers = entry.getCodeSigners();
        if (signers == null || signers.length != 1) throw new SecurityException("Unsigned or multiply signed entry: " + entry.getName());
        var cert = signers[0].getSignerCertPath().getCertificates().get(0);
        var sha = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(cert.getEncoded()));
        if (!sha.equalsIgnoreCase(args[1])) throw new SecurityException("Unexpected signer: " + entry.getName());
        count++;
      }
    }
    if (count < 10) throw new SecurityException("Incomplete bundle");
    System.out.println("PASS: " + count + " AAB data entries verified with the expected signing certificate");
  }
}
