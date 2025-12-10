package com.example.qrcodegenerator.controller;

import com.example.qrcodegenerator.service.QrService;
import com.google.zxing.WriterException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class QrController {

    @Autowired
    private QrService qrService;

    @Value("${app.public-url:}")
    private String publicUrl;

    private Integer parseColor(String hex) {
        if (hex == null)
            return null;
        String h = hex.trim();
        if (h.isEmpty())
            return null;
        if (h.startsWith("#"))
            h = h.substring(1);
        if (h.length() == 6)
            h = "FF" + h;
        if (h.length() != 8)
            return null;
        try {
            long val = Long.parseLong(h, 16);
            return (int) val;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Integer[] colorsForTheme(String theme) {
        if (theme == null || theme.isBlank())
            return null;
        String t = theme.toLowerCase();
        if (t.equals("classic"))
            return new Integer[] { 0xFF000000, 0xFFFFFFFF };
        if (t.equals("indigo"))
            return new Integer[] { 0xFF3F51B5, 0xFFFFFFFF };
        if (t.equals("sunset"))
            return new Integer[] { 0xFFDC2743, 0xFFFFF5F5 };
        if (t.equals("forest"))
            return new Integer[] { 0xFF2E7D32, 0xFFE8F5E9 };
        if (t.equals("midnight"))
            return new Integer[] { 0xFF000000, 0xFFE0E7FF };
        return null;
    }

    @GetMapping(value = "/qr", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> generateQr(@RequestParam("text") String text,
            @RequestParam(value = "size", required = false, defaultValue = "300") int size,
            @RequestParam(value = "theme", required = false) String theme,
            @RequestParam(value = "fg", required = false) String fg,
            @RequestParam(value = "bg", required = false) String bg) {
        try {
            Integer onColor = null;
            Integer offColor = null;
            Integer[] preset = colorsForTheme(theme);
            if (preset != null) {
                onColor = preset[0];
                offColor = preset[1];
            }
            Integer fgParsed = parseColor(fg);
            Integer bgParsed = parseColor(bg);
            if (fgParsed != null)
                onColor = fgParsed;
            if (bgParsed != null)
                offColor = bgParsed;

            byte[] png = (onColor != null || offColor != null)
                    ? qrService.generatePng(text, size, onColor, offColor)
                    : qrService.generatePng(text, size);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.IMAGE_PNG);
            headers.setContentLength(png.length);
            return new ResponseEntity<>(png, headers, HttpStatus.OK);
        } catch (WriterException | IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping(value = "/qr", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> generateQrPost(@RequestBody Map<String, Object> body,
            HttpServletRequest request) {
        try {
            String type = (String) body.getOrDefault("type", "text");
            int size = ((Number) body.getOrDefault("size", 300)).intValue();
            String content = "";
            String theme = (String) body.getOrDefault("theme", "");
            String fgColor = (String) body.getOrDefault("fgColor", "");
            String bgColor = (String) body.getOrDefault("bgColor", "");

            if ("text".equalsIgnoreCase(type)) {
                content = (String) body.getOrDefault("text", "");
            } else if ("url".equalsIgnoreCase(type)) {
                content = (String) body.getOrDefault("url", "");
            } else if ("social".equalsIgnoreCase(type)) {
                // Generate HTML landing page for social links
                @SuppressWarnings("unchecked")
                Map<String, String> payload = (Map<String, String>) body.getOrDefault("payload", Map.of());

                // Create HTML page
                StringBuilder html = new StringBuilder();
                html.append("<!DOCTYPE html>\n");
                html.append("<html>\n<head>\n");
                html.append(
                        "<meta charset='utf-8'><meta name='viewport' content='width=device-width, initial-scale=1'>\n");
                html.append("<title>Social Links</title>\n");
                html.append("<style>\n");
                html.append(
                        "body { font-family: Arial, sans-serif; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); ");
                html.append(
                        "min-height: 100vh; display: flex; align-items: center; justify-content: center; margin: 0; }\n");
                html.append(
                        ".container { background: white; border-radius: 16px; padding: 40px; box-shadow: 0 20px 60px rgba(0,0,0,0.3); ");
                html.append("max-width: 400px; text-align: center; }\n");
                html.append("h1 { color: #333; margin-bottom: 30px; }\n");
                html.append(".social-links { display: flex; flex-direction: column; gap: 12px; }\n");
                html.append("a { padding: 14px 24px; border-radius: 8px; text-decoration: none; font-weight: 600; ");
                html.append("transition: all 0.3s ease; display: block; }\n");
                html.append("a:hover { transform: translateY(-2px); box-shadow: 0 10px 25px rgba(0,0,0,0.2); }\n");
                html.append(".facebook { background: #1877f2; color: white; }\n");
                html.append(".twitter { background: #000; color: white; }\n");
                html.append(
                        ".instagram { background: linear-gradient(45deg, #f09433 0%,#e6683c 25%,#dc2743 50%,#cc2366 75%,#bc1888 100%); color: white; }\n");
                html.append(".linkedin { background: #0a66c2; color: white; }\n");
                html.append("</style>\n</head>\n<body>\n");
                html.append("<div class='container'>\n");
                html.append("<h1>Follow Me</h1>\n");
                html.append("<div class='social-links'>\n");

                if (payload.containsKey("facebook") && !payload.get("facebook").isBlank()) {
                    html.append("<a href='").append(payload.get("facebook"))
                            .append("' class='facebook' target='_blank'>Facebook</a>\n");
                }
                if (payload.containsKey("twitter") && !payload.get("twitter").isBlank()) {
                    html.append("<a href='").append(payload.get("twitter"))
                            .append("' class='twitter' target='_blank'>Twitter / X</a>\n");
                }
                if (payload.containsKey("instagram") && !payload.get("instagram").isBlank()) {
                    html.append("<a href='").append(payload.get("instagram"))
                            .append("' class='instagram' target='_blank'>Instagram</a>\n");
                }
                if (payload.containsKey("linkedin") && !payload.get("linkedin").isBlank()) {
                    html.append("<a href='").append(payload.get("linkedin"))
                            .append("' class='linkedin' target='_blank'>LinkedIn</a>\n");
                }

                html.append("</div>\n</div>\n</body>\n</html>\n");

                // Save HTML file
                String pagesDir = System.getProperty("user.dir") + File.separator + "pages";
                File dir = new File(pagesDir);
                if (!dir.exists())
                    dir.mkdirs();

                String pageId = System.currentTimeMillis() + "-" + (int) (Math.random() * 10000);
                File htmlFile = new File(dir, pageId + ".html");
                try (OutputStream os = new FileOutputStream(htmlFile)) {
                    os.write(html.toString().getBytes(StandardCharsets.UTF_8));
                }

                // QR code points to the HTML page
                String baseUrl = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort();
                String pageUrl = baseUrl + "/pages/" + pageId + ".html";
                content = pageUrl;
            } else if ("vcard".equalsIgnoreCase(type)) {
                @SuppressWarnings("unchecked")
                Map<String, String> payload = (Map<String, String>) body.getOrDefault("payload", Map.of());
                String fn = payload.getOrDefault("firstName", "");
                String ln = payload.getOrDefault("lastName", "");
                String org = payload.getOrDefault("org", "");
                String title = payload.getOrDefault("title", "");
                String phone = payload.getOrDefault("phone", "");
                String email = payload.getOrDefault("email", "");
                String urlv = payload.getOrDefault("url", "");
                String addr = payload.getOrDefault("address", "");

                StringBuilder v = new StringBuilder();
                v.append("BEGIN:VCARD\n");
                v.append("VERSION:3.0\n");
                v.append("N:").append(ln).append(";").append(fn).append("\n");
                v.append("FN:").append(fn).append(" ").append(ln).append("\n");
                if (!org.isBlank())
                    v.append("ORG:").append(org).append("\n");
                if (!title.isBlank())
                    v.append("TITLE:").append(title).append("\n");
                if (!phone.isBlank())
                    v.append("TEL:").append(phone).append("\n");
                if (!email.isBlank())
                    v.append("EMAIL:").append(email).append("\n");
                if (!addr.isBlank())
                    v.append("ADR:").append(addr).append("\n");
                if (!urlv.isBlank())
                    v.append("URL:").append(urlv).append("\n");
                v.append("END:VCARD\n");
                content = v.toString();
            } else if ("imageUrl".equalsIgnoreCase(type)) {
                String imageUrl = (String) body.getOrDefault("imageUrl", "");

                // Extract just the path part if it's a full URL
                String imagePath = imageUrl;
                if (imageUrl.contains("://")) {
                    // Extract path from full URL (e.g., "http://localhost:9091/uploads/file.jpg" ->
                    // "/uploads/file.jpg")
                    imagePath = imageUrl.substring(imageUrl.indexOf("/", imageUrl.indexOf("://") + 3));
                }

                // Create HTML page that displays the image
                StringBuilder html = new StringBuilder();
                html.append("<!DOCTYPE html>\n");
                html.append("<html>\n<head>\n");
                html.append(
                        "<meta charset='utf-8'><meta name='viewport' content='width=device-width, initial-scale=1'>\n");
                html.append("<title>Image</title>\n");
                html.append("<style>\n");
                html.append("* { margin: 0; padding: 0; box-sizing: border-box; }\n");
                html.append(
                        "body { font-family: Arial, sans-serif; background: #f0f0f0; min-height: 100vh; display: flex; align-items: center; justify-content: center; }\n");
                html.append(
                        ".container { background: white; border-radius: 12px; padding: 20px; box-shadow: 0 10px 40px rgba(0,0,0,0.2); max-width: 90vw; }\n");
                html.append("img { max-width: 100%; height: auto; border-radius: 8px; display: block; }\n");
                html.append(
                        ".download-btn { display: block; margin-top: 20px; padding: 12px 24px; background: #667eea; color: white; text-decoration: none; border-radius: 8px; text-align: center; font-weight: 600; transition: all 0.3s ease; }\n");
                html.append(".download-btn:hover { background: #764ba2; transform: translateY(-2px); }\n");
                html.append("</style>\n</head>\n<body>\n");
                html.append("<div class='container'>\n");

                // Check for ngrok URL in headers or use configured public URL
                String baseUrl = publicUrl;
                if (baseUrl == null || baseUrl.isEmpty()) {
                    baseUrl = request.getHeader("x-forwarded-proto") != null
                            ? request.getHeader("x-forwarded-proto") + "://" + request.getHeader("x-forwarded-host")
                            : request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort();
                }

                html.append("<img src='").append(baseUrl).append(imagePath).append("' alt='QR Code Image'>\n");
                html.append("<a href='").append(baseUrl).append(imagePath)
                        .append("' download class='download-btn'>Download Image</a>\n");
                html.append("</div>\n</body>\n</html>\n");

                // Save HTML file
                String pagesDir = System.getProperty("user.dir") + File.separator + "pages";
                File dir = new File(pagesDir);
                if (!dir.exists())
                    dir.mkdirs();

                String pageId = System.currentTimeMillis() + "-" + (int) (Math.random() * 10000);
                File htmlFile = new File(dir, pageId + ".html");
                try (OutputStream os = new FileOutputStream(htmlFile)) {
                    os.write(html.toString().getBytes(StandardCharsets.UTF_8));
                }

                // QR code points to the image page
                String pageUrl = baseUrl + "/pages/" + pageId + ".html";
                content = pageUrl;
            } else {
                content = (String) body.getOrDefault("text", "");
            }

            Integer onColor = null;
            Integer offColor = null;
            Integer[] preset = colorsForTheme(theme);
            if (preset != null) {
                onColor = preset[0];
                offColor = preset[1];
            }
            Integer fgParsed = parseColor(fgColor);
            Integer bgParsed = parseColor(bgColor);
            if (fgParsed != null)
                onColor = fgParsed;
            if (bgParsed != null)
                offColor = bgParsed;

            byte[] png = (onColor != null || offColor != null)
                    ? qrService.generatePng(content, size, onColor, offColor)
                    : qrService.generatePng(content, size);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.IMAGE_PNG);
            headers.setContentLength(png.length);
            return new ResponseEntity<>(png, headers, HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/upload-image")
    public ResponseEntity<?> uploadImage(@RequestParam("file") MultipartFile file, HttpServletRequest request) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "file is empty"));
        }

        try {
            String uploadsDir = System.getProperty("user.dir") + File.separator + "uploads";
            File dir = new File(uploadsDir);
            if (!dir.exists())
                dir.mkdirs();

            String original = StringUtils.cleanPath(file.getOriginalFilename());
            String filename = System.currentTimeMillis() + "-" + URLEncoder.encode(original, StandardCharsets.UTF_8);
            File dest = new File(dir, filename);
            try (OutputStream os = new FileOutputStream(dest)) {
                os.write(file.getBytes());
            }

            String baseUrl = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort();
            String url = baseUrl + "/uploads/" + filename;
            return ResponseEntity.ok(Map.of("url", url));
        } catch (IOException ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "save_failed"));
        }
    }
}
