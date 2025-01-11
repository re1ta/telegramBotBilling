package ibatov.telegramBot.AlphaBilling;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Contact;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Component
@RequiredArgsConstructor
public class Bot extends TelegramLongPollingBot {

    @Value("${bot.token}")
    private String tokenTelegram;
    @Value("${bot.name}")
    private String botName;
    @Value("${bot.secretKey}")
    private String key;

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage()) {
            Message updateChat = update.getMessage();
            if (updateChat.hasContact()) {
                Contact contact = updateChat.getContact();
                String phoneNumber = contact.getPhoneNumber();
                authenticateNumber(phoneNumber,updateChat);
            } else if (updateChat.hasText()) {
                String message = updateChat.getText();
                long chatId = updateChat.getChatId();
                switch (message) {
                    case "/start":
                        startCommend(chatId, updateChat.getChat().getFirstName());
                        break;
                    default:
                        sendMessage(chatId, "Простите, " + updateChat.getChat().getFirstName() + ", я такой команды не знаю.");
                }
            }
        }
    }

    private void startCommend(long chatId, String name) {
        String answer = "Здравствуй, " + name + "! Чтобы зайти на AlphaBilling вам нужно подтвердить номер телефона.";
        sendMessage(chatId, answer);
    }

    private void authenticateNumber(String phoneNumber, Message updateChat) {
        String apiUrlCheck = "http://host.docker.internal:8080/auth/checkPhone";
        String apiUrlSend = "http://host.docker.internal:8080/auth/sendCodeTelegram";
        if(!phoneNumber.contains("+")){
            phoneNumber = "+" + phoneNumber;
        }
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Object> requestEntity = new HttpEntity<>(new PhoneDto(phoneNumber), headers);
        System.out.printf(phoneNumber);
        ResponseEntity<Boolean> responseEntity = restTemplate.exchange(apiUrlCheck, HttpMethod.POST, requestEntity, Boolean.class);
        if (Boolean.TRUE.equals(responseEntity.getBody())){
            requestEntity = new HttpEntity<>(phoneNumber);
            ResponseEntity<String> responseEntityCode = restTemplate.exchange(apiUrlSend,HttpMethod.POST, requestEntity, String.class);
            sendMessage(updateChat.getChatId(), "Ваш код для входа: " + responseEntityCode.getBody());
        }
        else {
            sendMessage(updateChat.getChatId(), "Такого номера в базе нету!!!!");
        }
    }

    private void sendMessage(long chatId, String textToSend) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(textToSend);
        message.setReplyMarkup(makeKeyboard());
        try {
            execute(message);
        } catch (TelegramApiException e) {
        }
    }

    private ReplyKeyboardMarkup makeKeyboard() {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setSelective(true);
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(true);
        List<KeyboardRow> keyboardRows = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();

        KeyboardButton keyboardButton1 = new KeyboardButton();
        keyboardButton1.setText("Подтвердить номер телефона!");
        keyboardButton1.setRequestContact(true);

        row.add(keyboardButton1);
        keyboardRows.add(row);

        keyboardMarkup.setKeyboard(keyboardRows);
        return keyboardMarkup;
    }

    @Override
    public String getBotUsername() {
        return botName;
    }

    @Override
    public String getBotToken() {
        return tokenTelegram;
    }
}

