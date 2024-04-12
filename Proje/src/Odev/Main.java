/**
*
* @author Yiğit Talha Adagülü - yigitadagulu@gmail.com
* @since 04.04.2024
* <p>
* ...
* </p>
*/


package Odev;

import org.eclipse.jgit.api.*;
import org.eclipse.jgit.api.errors.GitAPIException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;
import java.util.stream.Stream;
import java.text.DecimalFormat;

public class Main {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Lütfen işlem yapmak istediğiniz Github URL'sini giriniz: ");
        String url = scanner.nextLine();

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        LocalDateTime now = LocalDateTime.now();
        String directoryPath = "/tmp/repo" + dtf.format(now);

        try {
            Git.cloneRepository()
                    .setURI(url)
                    .setDirectory(new File(directoryPath))
                    .call();
            System.out.println("Klonlama işlemi başarılı");

            try (Stream<Path> paths = Files.walk(Paths.get(directoryPath))) {
                paths
                        .filter(Files::isRegularFile)
                        .filter(p -> p.toString().endsWith(".java"))
                        .forEach(path -> {
                            try {
                                if (containsClassKeyword(path.toFile())) {
                                    analyzeJavaFile(path.toFile());
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        });
            }
        } catch (GitAPIException | IOException e) {
            System.out.println("Bir hata meydana geldi: " + e.getMessage());
        }
    }

    private static boolean containsClassKeyword(File file) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            boolean inComment = false;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("/*") && !line.endsWith("*/")) {
                    inComment = true;
                } else if (line.endsWith("*/")) {
                    inComment = false;
                }
                if (!inComment && line.contains("class ")) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void analyzeJavaFiles(File folder) throws IOException {
        for (File file : folder.listFiles()) {
            if (file.isDirectory()) {
                analyzeJavaFiles(file);
            } else if (file.getName().endsWith(".java")) {
                analyzeJavaFile(file);
            }
        }
    }

    private static void analyzeJavaFile(File file) throws IOException {
        int javadocYorumlar = 0;
        int digerYorumlar = 0;
        int kodSatir = 0;
        int totalSatir = 0;
        int fonksiyonSayisi = 0;

        BufferedReader reader = new BufferedReader(new FileReader(file));
        String line;
        boolean inJavadocYorumlar = false;
        boolean inDigerYorumlar = false;
        while ((line = reader.readLine()) != null) {
            totalSatir++;
            line = line.trim();
            if (!line.isEmpty() && !line.startsWith("//") && !line.startsWith("/*") && !line.startsWith("*") && !line.startsWith("/**") && !line.endsWith("*/")) {
                kodSatir++;
            }
            if (line.startsWith("/**")) {
                inJavadocYorumlar = true;
                continue; // Skip this line
            }
            if (inJavadocYorumlar) {
                if (line.endsWith("*/")) {
                    inJavadocYorumlar = false; // End of Javadoc comment
                    continue; // Skip this line
                }
                javadocYorumlar++;
            } else if (line.contains("//")) {
                digerYorumlar++;
            } else if (line.startsWith("/*")) {
                inDigerYorumlar = true;
                if (line.endsWith("*/")) {
                    inDigerYorumlar = false;
                }
                digerYorumlar++;
            } else if ((line.contains("public") || line.contains("private") || line.contains("protected")) && (line.contains("(") || line.contains(")"))) {
                fonksiyonSayisi++;
            }
        }
        reader.close();

        double YG = ((javadocYorumlar + digerYorumlar) * 0.8) / fonksiyonSayisi;
        double YH = (kodSatir / (double) fonksiyonSayisi) * 0.3;
        double sapmaYuzdesi = ((100 * YG) / YH) - 100;
        DecimalFormat df = new DecimalFormat("#.##");

        System.out.println("-------------------------------------------------");
        System.out.println("Dosya: " + file.getName());
        System.out.println("Javadoc yorum satır sayısı: " + javadocYorumlar);
        System.out.println("Diğer yorum satır sayısı: " + digerYorumlar);
        System.out.println("Kod satır sayısı: " + kodSatir);
        System.out.println("Toplam Satır sayısı: " + totalSatir);
        System.out.println("Fonksiyon sayısı: " + fonksiyonSayisi);
        System.out.println("Yorum Sapma Yüzdesi: " + df.format(sapmaYuzdesi) + "%");
    }
}