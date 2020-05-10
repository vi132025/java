package Downloader;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Properties;
import java.util.stream.Collectors;

public class Main {
    public static void main(String[] args) {

        System.out.print("Запуск программы\n");

        Properties config = new Properties();
        try (FileInputStream input = new FileInputStream("init.properties")) {
            config.load(input);
        } catch (IOException ex) {
            System.out.print("Произошла ошибка при чтении конфигурационного файла\n");
            ex.printStackTrace();
            return;
        }

        File directory = new File(config.getProperty("Folder"));
        if (! directory.exists()){
            directory.mkdir();
        }

        HashSet<String> downloadedUrls = new HashSet<String>();
        ArrayDeque<String> needDownloadUrls = new ArrayDeque<String>();
        needDownloadUrls.push(config.getProperty("Url"));
        int counter = 0;
        while(!needDownloadUrls.isEmpty()){
            String url = needDownloadUrls.pop();

            //не хотим дважды скачивать одинаковые страницы
            if (downloadedUrls.contains(url)){
                continue;
            }

            ArrayList<String> result = DownloadPage(config, url);
            downloadedUrls.add(url);
            needDownloadUrls.addAll(result);

            //выводим на экран прогресс
            counter++;
            if(counter >= 100){
                counter = 0;
                System.out.print("Обработано " + downloadedUrls.size() + " страниц. " + needDownloadUrls.size() + " в очереди\n");
            }
        }

        System.out.print("Конец работы программы\n");
    }

    private static ArrayList<String> DownloadPage(Properties config, String url){
        try {
            Document doc = Jsoup.connect(url).get();
            ArrayList<String> pages = new ArrayList<String>(doc.select("a[href]").stream().map(element -> element.absUrl("href")).distinct().collect(Collectors.toList())) ;
            ArrayList<String> images = new ArrayList<String>(doc.select("img[src]").stream().map(element -> element.absUrl("src")).distinct().collect(Collectors.toList()));

            DownloadImages(config, images);

            return pages;
        }
        catch (Exception ex){
            System.out.print("Произошла ошибка при скачивании страницы " + url + "\n");
            ex.printStackTrace();

            return new ArrayList<String>();
        }
    }

    private static void DownloadImages(Properties config, ArrayList<String> images){
        for (String url : images) {
            try {
                URL u = new URL(url);

                byte[] bytes = u.openStream().readAllBytes();

                if(bytes.length < Integer.parseInt(config.getProperty("FileSize"))){
                    continue;
                }
                String fileName = url.replaceAll("[^a-zA-Z0-9.-]", "_");

                Path path = Path.of(config.getProperty("Folder"), fileName);
                Files.write(path, bytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            }
            catch (Exception ex){
                System.out.print("Произошла ошибка при скачивании и сохранении изображения " + url + "\n");
                ex.printStackTrace();
            }
        }
    }
}
