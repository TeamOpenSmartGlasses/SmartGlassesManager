package com.teamopensmartglasses.smartglassesmanager.speechrecognition.azure;

import android.content.Context;
import android.util.Log;

import com.microsoft.cognitiveservices.speech.Connection;
import com.microsoft.cognitiveservices.speech.PhraseListGrammar;
import com.microsoft.cognitiveservices.speech.ProfanityOption;
import com.microsoft.cognitiveservices.speech.SpeechConfig;
import com.microsoft.cognitiveservices.speech.SpeechRecognizer;
import com.microsoft.cognitiveservices.speech.audio.AudioConfig;
import com.teamopensmartglasses.smartglassesmanager.eventbusmessages.SpeechRecOutputEvent;
import com.teamopensmartglasses.smartglassesmanager.speechrecognition.SpeechRecFramework;
import java.math.BigInteger;

import org.greenrobot.eventbus.EventBus;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class SpeechRecAzure extends SpeechRecFramework {
    private static final String API_KEY = "0a2244c410664011bbf33fdb2cdc0f30";
    private static final String REGION = "eastasia";
    private static final String TAG = "WearableAi_SpeechRecAzure";

    private SpeechConfig speechConfig;
    private SpeechRecognizer speechRecognizer;
    private ExecutorService executorService = Executors.newCachedThreadPool();
    private Context mContext;
    private String currentLanguageCode;

    public SpeechRecAzure(Context context, String languageLocale) {
        this.mContext = context;
        initLanguageLocale(languageLocale);
    }

    @Override
    public void start() {
        Log.d(TAG, "Starting Azure Speech Recognition");
        initializeRecognizer();
    }

    private void stopReco() {
        Log.d(TAG, "Attempting to stop continuous recognition.");
        if (speechRecognizer != null) {
            executorService.submit(() -> {
                try {
                    speechRecognizer.stopContinuousRecognitionAsync().get();
                    Log.i(TAG, "Continuous recognition stopped.");
                } catch (Exception e) {
                    Log.e(TAG, "Failed to stop continuous recognition: " + e.getMessage());
                } finally {
                    speechRecognizer.close();
                    speechRecognizer = null;
                    Log.i(TAG, "SpeechRecognizer instance closed and set to null.");
//                    runOnUiThread(() -> startRecoButton.setEnabled(true));
                    AzureAudioInputStream.getInstance().close();
                }
            });
        }
    }

    @Override
    public void destroy() {
        stopReco();
        Log.d(TAG, "--- Azure speech rec destroyed.");
    }

    @Override
    public void ingestAudioChunk(byte[] audioChunk) {
        if (speechRecognizer != null) {
            AzureAudioInputStream.getInstance().push(audioChunk);
        }
    }

    private void initializeRecognizer() {
        speechConfig = SpeechConfig.fromSubscription(API_KEY, REGION);
        speechConfig.setSpeechRecognitionLanguage(currentLanguageCode);
        speechConfig.requestWordLevelTimestamps();
        speechConfig.setProfanity(ProfanityOption.Raw);

        AudioConfig audioConfig = AudioConfig.fromStreamInput(AzureAudioInputStream.getInstance());

        speechRecognizer = new SpeechRecognizer(speechConfig, audioConfig);

        setupPhraseList(speechRecognizer);

        speechRecognizer.recognizing.addEventListener((o, e) -> {
            String intermediateResult = e.getResult().getText();
            BigInteger offset = e.getResult().getOffset();
            if (intermediateResult != null && !intermediateResult.trim().isEmpty()) {
                EventBus.getDefault().post(new SpeechRecOutputEvent(intermediateResult, offset.longValue(), false));
                Log.d(TAG, "Got intermediate ASR: " + intermediateResult);
            }
        });

        speechRecognizer.recognized.addEventListener((o, e) -> {
            String finalResult = e.getResult().getText();
            BigInteger offset = e.getResult().getOffset();
            if (finalResult != null && !finalResult.trim().isEmpty()) {
                EventBus.getDefault().post(new SpeechRecOutputEvent(finalResult, offset.longValue(), true));
                Log.d(TAG, "Got final ASR: " + finalResult);
            }
        });

        Connection connection = Connection.fromRecognizer(speechRecognizer);
        connection.disconnected.addEventListener((s, connectionEventArgs) -> {
            Log.e(TAG, "Disconnected from Azure Speech Service, sessionId: " + connectionEventArgs.getSessionId());
            handleDisconnect();
        });

        executorService.submit(() -> {
            try {
                speechRecognizer.startContinuousRecognitionAsync().get();
                Log.i(TAG, "Continuous recognition started.");
            } catch (Exception e) {
                Log.e(TAG, "Error starting continuous recognition: " + e.getMessage());
                handleDisconnect();
            }
        });
    }

    private void handleDisconnect() {
        executorService.submit(() -> {
            boolean connected = false;
            while (!connected) {
                try {
                    Thread.sleep(1000);
                    speechRecognizer.startContinuousRecognitionAsync().get();
                    connected = true;
                    Log.i(TAG, "Reconnected and continuous recognition started.");
                } catch (Exception e) {
                    Log.e(TAG, "Error reconnecting: " + e.getMessage());
                }
            }
        });
    }

    private void setupPhraseList(SpeechRecognizer speechRecognizer) {
        PhraseListGrammar phraseListGrammar = PhraseListGrammar.fromRecognizer(speechRecognizer);
        phraseListGrammar.addPhrase("Hey Mira");
        phraseListGrammar.addPhrase("Convoscope");
        phraseListGrammar.addPhrase("Team Open Smart Glasses");
        phraseListGrammar.addPhrase("smart glasses");
    }



















    //below is long list of languages
    private void initLanguageLocale(String localeString) {
        switch (localeString) {
            case "Afrikaans (South Africa)":
                currentLanguageCode = "af-ZA";
                break;
            case "Amharic (Ethiopia)":
                currentLanguageCode = "am-ET";
                break;
            case "Arabic (United Arab Emirates)":
                currentLanguageCode = "ar-AE";
                break;
            case "Arabic (Bahrain)":
                currentLanguageCode = "ar-BH";
                break;
            case "Arabic (Algeria)":
                currentLanguageCode = "ar-DZ";
                break;
            case "Arabic (Egypt)":
                currentLanguageCode = "ar-EG";
                break;
            case "Arabic (Israel)":
                currentLanguageCode = "ar-IL";
                break;
            case "Arabic (Iraq)":
                currentLanguageCode = "ar-IQ";
                break;
            case "Arabic (Jordan)":
                currentLanguageCode = "ar-JO";
                break;
            case "Arabic (Kuwait)":
                currentLanguageCode = "ar-KW";
                break;
            case "Arabic (Lebanon)":
                currentLanguageCode = "ar-LB";
                break;
            case "Arabic (Libya)":
                currentLanguageCode = "ar-LY";
                break;
            case "Arabic (Morocco)":
                currentLanguageCode = "ar-MA";
                break;
            case "Arabic (Oman)":
                currentLanguageCode = "ar-OM";
                break;
            case "Arabic (Palestinian Authority)":
                currentLanguageCode = "ar-PS";
                break;
            case "Arabic (Qatar)":
                currentLanguageCode = "ar-QA";
                break;
            case "Arabic (Saudi Arabia)":
                currentLanguageCode = "ar-SA";
                break;
            case "Arabic (Syria)":
                currentLanguageCode = "ar-SY";
                break;
            case "Arabic (Tunisia)":
                currentLanguageCode = "ar-TN";
                break;
            case "Arabic (Yemen)":
                currentLanguageCode = "ar-YE";
                break;
            case "Azerbaijani (Latin, Azerbaijan)":
                currentLanguageCode = "az-AZ";
                break;
            case "Bulgarian (Bulgaria)":
                currentLanguageCode = "bg-BG";
                break;
            case "Bengali (India)":
                currentLanguageCode = "bn-IN";
                break;
            case "Bosnian (Bosnia and Herzegovina)":
                currentLanguageCode = "bs-BA";
                break;
            case "Catalan":
                currentLanguageCode = "ca-ES";
                break;
            case "Czech (Czechia)":
                currentLanguageCode = "cs-CZ";
                break;
            case "Welsh (United Kingdom)":
                currentLanguageCode = "cy-GB";
                break;
            case "Danish (Denmark)":
                currentLanguageCode = "da-DK";
                break;
            case "German (Austria)":
                currentLanguageCode = "de-AT";
                break;
            case "German (Switzerland)":
                currentLanguageCode = "de-CH";
                break;
            case "German":
            case "German (Germany)":
                currentLanguageCode = "de-DE";
                break;
            case "Greek (Greece)":
                currentLanguageCode = "el-GR";
                break;
            case "English (Australia)":
                currentLanguageCode = "en-AU";
                break;
            case "English (Canada)":
                currentLanguageCode = "en-CA";
                break;
            case "English (United Kingdom)":
                currentLanguageCode = "en-GB";
                break;
            case "English (Ghana)":
                currentLanguageCode = "en-GH";
                break;
            case "English (Hong Kong SAR)":
                currentLanguageCode = "en-HK";
                break;
            case "English (Ireland)":
                currentLanguageCode = "en-IE";
                break;
            case "English (India)":
                currentLanguageCode = "en-IN";
                break;
            case "English (Kenya)":
                currentLanguageCode = "en-KE";
                break;
            case "English (Nigeria)":
                currentLanguageCode = "en-NG";
                break;
            case "English (New Zealand)":
                currentLanguageCode = "en-NZ";
                break;
            case "English (Philippines)":
                currentLanguageCode = "en-PH";
                break;
            case "English (Singapore)":
                currentLanguageCode = "en-SG";
                break;
            case "English (Tanzania)":
                currentLanguageCode = "en-TZ";
                break;
            case "English":
            case "English (United States)":
                currentLanguageCode = "en-US";
                break;
            case "English (South Africa)":
                currentLanguageCode = "en-ZA";
                break;
            case "Spanish (Argentina)":
                currentLanguageCode = "es-AR";
                break;
            case "Spanish (Bolivia)":
                currentLanguageCode = "es-BO";
                break;
            case "Spanish (Chile)":
                currentLanguageCode = "es-CL";
                break;
            case "Spanish (Colombia)":
                currentLanguageCode = "es-CO";
                break;
            case "Spanish (Costa Rica)":
                currentLanguageCode = "es-CR";
                break;
            case "Spanish (Cuba)":
                currentLanguageCode = "es-CU";
                break;
            case "Spanish (Dominican Republic)":
                currentLanguageCode = "es-DO";
                break;
            case "Spanish (Ecuador)":
                currentLanguageCode = "es-EC";
                break;
            case "Spanish (Spain)":
                currentLanguageCode = "es-ES";
                break;
            case "Spanish (Equatorial Guinea)":
                currentLanguageCode = "es-GQ";
                break;
            case "Spanish (Guatemala)":
                currentLanguageCode = "es-GT";
                break;
            case "Spanish (Honduras)":
                currentLanguageCode = "es-HN";
                break;
            case "Spanish":
            case "Spanish (Mexico)":
                currentLanguageCode = "es-MX";
                break;
            case "Spanish (Nicaragua)":
                currentLanguageCode = "es-NI";
                break;
            case "Spanish (Panama)":
                currentLanguageCode = "es-PA";
                break;
            case "Spanish (Peru)":
                currentLanguageCode = "es-PE";
                break;
            case "Spanish (Puerto Rico)":
                currentLanguageCode = "es-PR";
                break;
            case "Spanish (Paraguay)":
                currentLanguageCode = "es-PY";
                break;
            case "Spanish (El Salvador)":
                currentLanguageCode = "es-SV";
                break;
            case "Spanish (United States)":
                currentLanguageCode = "es-US";
                break;
            case "Spanish (Uruguay)":
                currentLanguageCode = "es-UY";
                break;
            case "Spanish (Venezuela)":
                currentLanguageCode = "es-VE";
                break;
            case "Estonian (Estonia)":
                currentLanguageCode = "et-EE";
                break;
            case "Basque":
                currentLanguageCode = "eu-ES";
                break;
            case "Persian (Iran)":
                currentLanguageCode = "fa-IR";
                break;
            case "Finnish (Finland)":
                currentLanguageCode = "fi-FI";
                break;
            case "Filipino (Philippines)":
                currentLanguageCode = "fil-PH";
                break;
            case "French (Belgium)":
                currentLanguageCode = "fr-BE";
                break;
            case "French (Canada)":
                currentLanguageCode = "fr-CA";
                break;
            case "French (Switzerland)":
                currentLanguageCode = "fr-CH";
                break;
            case "French":
            case "French (France)":
                currentLanguageCode = "fr-FR";
                break;
            case "Irish (Ireland)":
                currentLanguageCode = "ga-IE";
                break;
            case "Galician":
                currentLanguageCode = "gl-ES";
                break;
            case "Gujarati (India)":
                currentLanguageCode = "gu-IN";
                break;
            case "Hebrew":
            case "Hebrew (Israel)":
                currentLanguageCode = "he-IL";
                break;
            case "Hindi (India)":
                currentLanguageCode = "hi-IN";
                break;
            case "Croatian (Croatia)":
                currentLanguageCode = "hr-HR";
                break;
            case "Hungarian (Hungary)":
                currentLanguageCode = "hu-HU";
                break;
            case "Armenian (Armenia)":
                currentLanguageCode = "hy-AM";
                break;
            case "Indonesian (Indonesia)":
                currentLanguageCode = "id-ID";
                break;
            case "Icelandic (Iceland)":
                currentLanguageCode = "is-IS";
                break;
            case "Italian (Switzerland)":
                currentLanguageCode = "it-CH";
                break;
            case "Italian":
            case "Italian (Italy)":
                currentLanguageCode = "it-IT";
                break;
            case "Japanese":
            case "Japanese (Japan)":
                currentLanguageCode = "ja-JP";
                break;
            case "Javanese (Latin, Indonesia)":
                currentLanguageCode = "jv-ID";
                break;
            case "Georgian (Georgia)":
                currentLanguageCode = "ka-GE";
                break;
            case "Kazakh (Kazakhstan)":
                currentLanguageCode = "kk-KZ";
                break;
            case "Khmer (Cambodia)":
                currentLanguageCode = "km-KH";
                break;
            case "Kannada (India)":
                currentLanguageCode = "kn-IN";
                break;
            case "Korean":
            case "Korean (Korea)":
                currentLanguageCode = "ko-KR";
                break;
            case "Lao (Laos)":
                currentLanguageCode = "lo-LA";
                break;
            case "Lithuanian (Lithuania)":
                currentLanguageCode = "lt-LT";
                break;
            case "Latvian (Latvia)":
                currentLanguageCode = "lv-LV";
                break;
            case "Macedonian (North Macedonia)":
                currentLanguageCode = "mk-MK";
                break;
            case "Malayalam (India)":
                currentLanguageCode = "ml-IN";
                break;
            case "Mongolian (Mongolia)":
                currentLanguageCode = "mn-MN";
                break;
            case "Marathi (India)":
                currentLanguageCode = "mr-IN";
                break;
            case "Malay (Malaysia)":
                currentLanguageCode = "ms-MY";
                break;
            case "Maltese (Malta)":
                currentLanguageCode = "mt-MT";
                break;
            case "Burmese (Myanmar)":
                currentLanguageCode = "my-MM";
                break;
            case "Norwegian Bokmål (Norway)":
                currentLanguageCode = "nb-NO";
                break;
            case "Nepali (Nepal)":
                currentLanguageCode = "ne-NP";
                break;
            case "Dutch":
            case "Dutch (Belgium)":
                currentLanguageCode = "nl-BE";
                break;
            case "Dutch (Netherlands)":
                currentLanguageCode = "nl-NL";
                break;
            case "Punjabi (India)":
                currentLanguageCode = "pa-IN";
                break;
            case "Polish (Poland)":
                currentLanguageCode = "pl-PL";
                break;
            case "Pashto (Afghanistan)":
                currentLanguageCode = "ps-AF";
                break;
            case "Portuguese":
            case "Portuguese (Brazil)":
                currentLanguageCode = "pt-BR";
                break;
            case "Portuguese (Portugal)":
                currentLanguageCode = "pt-PT";
                break;
            case "Romanian (Romania)":
                currentLanguageCode = "ro-RO";
                break;
            case "Russian (Russia)":
                currentLanguageCode = "ru-RU";
                break;
            case "Sinhala (Sri Lanka)":
                currentLanguageCode = "si-LK";
                break;
            case "Slovak (Slovakia)":
                currentLanguageCode = "sk-SK";
                break;
            case "Slovenian (Slovenia)":
                currentLanguageCode = "sl-SI";
                break;
            case "Somali (Somalia)":
                currentLanguageCode = "so-SO";
                break;
            case "Albanian (Albania)":
                currentLanguageCode = "sq-AL";
                break;
            case "Serbian (Cyrillic, Serbia)":
                currentLanguageCode = "sr-RS";
                break;
            case "Swedish (Sweden)":
                currentLanguageCode = "sv-SE";
                break;
            case "Swahili (Kenya)":
                currentLanguageCode = "sw-KE";
                break;
            case "Swahili (Tanzania)":
                currentLanguageCode = "sw-TZ";
                break;
            case "Tamil (India)":
                currentLanguageCode = "ta-IN";
                break;
            case "Telugu (India)":
                currentLanguageCode = "te-IN";
                break;
            case "Thai (Thailand)":
                currentLanguageCode = "th-TH";
                break;
            case "Turkish":
            case "Turkish (Türkiye)":
                currentLanguageCode = "tr-TR";
                break;
            case "Ukrainian (Ukraine)":
                currentLanguageCode = "uk-UA";
                break;
            case "Urdu (India)":
                currentLanguageCode = "ur-IN";
                break;
            case "Uzbek (Latin, Uzbekistan)":
                currentLanguageCode = "uz-UZ";
                break;
            case "Vietnamese (Vietnam)":
                currentLanguageCode = "vi-VN";
                break;
            case "Chinese (Wu, Simplified)":
                currentLanguageCode = "wuu-CN";
                break;
            case "Chinese (Cantonese, Simplified)":
                currentLanguageCode = "yue-CN";
                break;
            case "Chinese":
            case "Chinese (Pinyin)":
            case "Chinese (Hanzi)":
            case "Chinese (Mandarin, Simplified)":
                currentLanguageCode = "zh-CN";
                break;
            case "Chinese (Jilu Mandarin, Simplified)":
                currentLanguageCode = "zh-CN-shandong";
                break;
            case "Chinese (Southwestern Mandarin, Simplified)":
                currentLanguageCode = "zh-CN-sichuan";
                break;
            case "Chinese (Cantonese, Traditional)":
                currentLanguageCode = "zh-HK";
                break;
            case "Chinese (Taiwanese Mandarin, Traditional)":
                currentLanguageCode = "zh-TW";
                break;
            case "Zulu (South Africa)":
                currentLanguageCode = "zu-ZA";
                break;
            default:
                currentLanguageCode = "en-US";
                break;
        }
    }
}