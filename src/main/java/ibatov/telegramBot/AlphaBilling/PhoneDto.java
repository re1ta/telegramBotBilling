package ibatov.telegramBot.AlphaBilling;

import lombok.Data;

@Data
public class PhoneDto {

    private String phoneNumber;

    public PhoneDto(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
}
