package com.example.Ecommerce.Common;

import java.io.IOException;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;

@Service
public class CloudinaryService {

    private final Cloudinary cloudinary;

    public CloudinaryService(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    public String uploadImage(MultipartFile file) throws IOException {
        Map<?, ?> result = cloudinary.uploader().upload(
                file.getBytes(),
                ObjectUtils.asMap(
                        "folder", "ecommerce/products",
                        "resource_type", "image",
                        "transformation", "q_auto,f_auto,w_800"));
        return result.get("secure_url").toString();
    }

    public void deleteImage(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) return;
        try {
            String publicId = extractPublicId(imageUrl);
            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
        } catch (IOException ignored) {
        }
    }

    private String extractPublicId(String imageUrl) {
        // URL format: https://res.cloudinary.com/{cloud}/image/upload/{folder}/{file}.{ext}
        String withoutSuffix = imageUrl.substring(imageUrl.lastIndexOf("/") + 1);
        String name = withoutSuffix.substring(0, withoutSuffix.lastIndexOf("."));
        // strip folder prefix if present — we stored into "ecommerce/products"
        if (name.contains("/")) {
            return name;
        }
        return "ecommerce/products/" + name;
    }
}
