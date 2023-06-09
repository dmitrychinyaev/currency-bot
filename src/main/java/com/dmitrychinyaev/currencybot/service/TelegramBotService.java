package com.dmitrychinyaev.currencybot.service;

import com.dmitrychinyaev.currencybot.entity.ForeignCurrency;
import com.dmitrychinyaev.currencybot.entity.TelegramBotCommon;
import com.dmitrychinyaev.currencybot.repository.TelegramBotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.regex.Pattern;

@RequiredArgsConstructor
@Component
@Slf4j
public class TelegramBotService {
    private final TelegramBotRepository telegramBotRepository;
    public String makeCalculation(String userMessage) throws IOException, ParserConfigurationException, SAXException {
        if (Pattern.matches(TelegramBotCommon.REGEX_CONVERT_TO_RUB, userMessage)) {
            String[] splitUserMessage = userMessage.split(" ");
            //check if number < 1
            String numberToConvertFromMessage = splitUserMessage[0];
            if(checkNumberGreaterThanZero(numberToConvertFromMessage)<1){
                log.error("В запросе отрицательное число или ноль");
                return TelegramBotCommon.TEXT_NOT_POSITIVE;
            }
            // check if requested charcode exist
            String currencyCharCodeFromMessage = splitUserMessage[1];
            if(telegramBotRepository.findForeignCurrencyByCharCode(currencyCharCodeFromMessage)==null){
                log.error("Запрашиваемая валюта не найдена");
                return TelegramBotCommon.TEXT_NOT_FOUND_CURRENCY;
            }
            return String.format(TelegramBotCommon.FORMAT_RESULT_MESSAGE, currencyConversion(numberToConvertFromMessage,currencyCharCodeFromMessage,false), getDateOfUpdateCurrencyBase());
        }
        if (Pattern.matches(TelegramBotCommon.REGEX_CONVERT_RUB_TO_CURRENCY, userMessage)) {
            String[] splitUserMessage = userMessage.split(" ");
            //check if number < 1
            String numberToConvertFromMessage = splitUserMessage[0];
            if(checkNumberGreaterThanZero(numberToConvertFromMessage)<1 |
                    checkNumberGreaterThanZero(numberToConvertFromMessage)>100000000 ){
                log.error("В запросе отрицательное число или ноль");
                return TelegramBotCommon.TEXT_NOT_POSITIVE;
            }
            // check if requested charcode exist
            String currencyCharCodeFromMessage = splitUserMessage[2];
            if(telegramBotRepository.findForeignCurrencyByCharCode(currencyCharCodeFromMessage)==null){
                log.error("Запрашиваемая валюта не найдена");
                return TelegramBotCommon.TEXT_NOT_FOUND_CURRENCY;
            }
            return String.format(TelegramBotCommon.FORMAT_RESULT_MESSAGE, currencyConversion(numberToConvertFromMessage,currencyCharCodeFromMessage,true), getDateOfUpdateCurrencyBase());
        }
        log.error("Неверный запрос");
        return TelegramBotCommon.TEXT_INCORRECT_REQUEST;
    }

    public String currencyConversion(String numberToConvertFromMessage, String currencyCharCodeFromMessage, boolean convertRUBtoCURRENCY) throws IOException, ParserConfigurationException, SAXException {
        var numberRequestedToConvert = new BigDecimal(numberToConvertFromMessage);
        ForeignCurrency chosenCurrency = telegramBotRepository.findForeignCurrencyByCharCode(currencyCharCodeFromMessage);
        var valueForeignCurrency = BigDecimal.valueOf(chosenCurrency.getValue());
        var nominalForeignCurrency = BigDecimal.valueOf(chosenCurrency.getNominal());

        if(convertRUBtoCURRENCY){
            var conversionResult = numberRequestedToConvert.multiply(nominalForeignCurrency)
                    .divide(valueForeignCurrency, MathContext.DECIMAL128);
            return TelegramBotCommon.FORMAT_CONVERT_RUB_TO_CURRENCY.formatted(numberRequestedToConvert,conversionResult, chosenCurrency.getCharCode());
        }

        var conversionResult = numberRequestedToConvert.multiply(valueForeignCurrency)
                .divide(nominalForeignCurrency, MathContext.DECIMAL128);
        return TelegramBotCommon.FORMAT_CONVERT_TO_RUB.formatted(numberRequestedToConvert, chosenCurrency.getCharCode(), conversionResult);
    }
    public String getDateOfUpdateCurrencyBase() {
        return String.format(TelegramBotCommon.FORMAT_MESSAGE_DATE_OF_UPGRADE, telegramBotRepository.getStringDateOfUpdate());
    }

    public String getAvailableCurrency() throws IOException, ParserConfigurationException, SAXException {
        return telegramBotRepository.getListOfAvailableCurrency();
    }

    public int checkNumberGreaterThanZero(String number){
        return BigDecimal.valueOf(Double.parseDouble(number)).signum();
    }

}
