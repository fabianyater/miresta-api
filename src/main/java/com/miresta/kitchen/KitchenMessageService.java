package com.miresta.kitchen;

import com.miresta.auth.User;
import com.miresta.auth.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class KitchenMessageService {

    private static final int MAX_LENGTH = 200;

    private final KitchenMessageRepository repository;
    private final UserRepository userRepository;

    @Transactional
    public KitchenMessageResponse send(String rawText, String senderEmail) {
        String text = rawText == null ? "" : rawText.trim();
        if (text.isEmpty()) {
            throw new IllegalStateException("El mensaje no puede estar vacío.");
        }
        if (text.length() > MAX_LENGTH) {
            text = text.substring(0, MAX_LENGTH);
        }

        String sentBy = userRepository.findByEmail(senderEmail)
                .map(User::getDisplayName)
                .filter(name -> name != null && !name.isBlank())
                .orElse(senderEmail);

        KitchenMessage message = new KitchenMessage();
        message.setText(text);
        message.setSentBy(sentBy);
        message.setCreatedAt(Instant.now());

        return toResponse(repository.save(message));
    }

    /**
     * Con {@code since}, los mensajes posteriores a esa hora en orden cronológico (para
     * el sondeo de la pantalla de cocina); sin él, los últimos 20 como historial.
     */
    @Transactional(readOnly = true)
    public List<KitchenMessageResponse> list(Instant since) {
        List<KitchenMessage> messages;
        if (since != null) {
            messages = repository.findByCreatedAtGreaterThanOrderByCreatedAtAsc(since);
        } else {
            messages = new ArrayList<>(repository.findTop20ByOrderByCreatedAtDesc());
            Collections.reverse(messages);
        }
        return messages.stream().map(this::toResponse).toList();
    }

    private KitchenMessageResponse toResponse(KitchenMessage message) {
        return new KitchenMessageResponse(
                message.getId(), message.getText(), message.getSentBy(), message.getCreatedAt());
    }
}
