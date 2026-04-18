package com.reelvault;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * ReelVault — Personal Semantic Knowledge Base
 *
 * <p>Saves links, reels, notes, and resources with auto-generated vector embeddings
 * via Hugging Face. Supports semantic (natural language) search via pgvector cosine
 * similarity. Also runs a Telegram bot for quick saves and searches on the go.
 *
 * <p>Deployment:
 * <ul>
 *   <li>Backend: Railway.app — java -jar target/reelvault.jar</li>
 *   <li>Database: Supabase (PostgreSQL + pgvector)</li>
 *   <li>Frontend: Vercel</li>
 * </ul>
 */
@SpringBootApplication
public class ReelVaultApplication {

    public static void main(String[] args) {
        SpringApplication.run(ReelVaultApplication.class, args);
    }
}
