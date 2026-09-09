package br.com.jarbas.gol;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.speech.tts.Voice;
import android.view.KeyEvent;
import android.os.Bundle;
import android.provider.Settings;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends Activity implements TextToSpeech.OnInitListener {
    private SpeechRecognizer recognizer;
    private Intent speechIntent;
    private TextToSpeech tts;
    private TextView status;
    private AudioManager audioManager;
    private boolean conversation = false;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_main);

        status = findViewById(R.id.status);
        Button listen = findViewById(R.id.listen);
        Button stop = findViewById(R.id.stop);

        tts = new TextToSpeech(this, this);
        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);

        if (android.os.Build.VERSION.SDK_INT >= 23 &&
            checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, 10);
        }

        speechIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        speechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        speechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "pt-BR");
        speechIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false);
        speechIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
        speechIntent.putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true);
        speechIntent.putExtra("android.speech.extra.BEEP_SOUND", false);

        recognizer = SpeechRecognizer.createSpeechRecognizer(this);
        recognizer.setRecognitionListener(new RecognitionListener() {
            public void onReadyForSpeech(Bundle p) { status.setText("JARBAS em espera silenciosa."); }
            public void onBeginningOfSpeech() { status.setText("..."); }
            public void onRmsChanged(float r) {}
            public void onBufferReceived(byte[] b) {}
            public void onEndOfSpeech() { status.setText("Processando..."); }
            public void onError(int e) {
                if (conversation) {
                    status.setText("Modo mãos-livres ativo.");
                    status.postDelayed(() -> listenAgain(), 500);
                } else {
                    status.setText("Toque em OUVIR JARBAS para ativar.");
                }
            }
            public void onResults(Bundle r) {
                ArrayList<String> a = r.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                String heard = (a != null && !a.isEmpty()) ? a.get(0) : "";
                respond(heard);
            }
            public void onPartialResults(Bundle r) {}
            public void onEvent(int t, Bundle p) {}
        });

        listen.setOnClickListener(v -> {
            conversation = true;
            status.setText("Modo mãos-livres ativo.");
            listenAgain();
        });

        stop.setOnClickListener(v -> {
            conversation = false;
            recognizer.cancel();
            status.setText("JARBAS em espera.");
        });
    }

    private void listenAgain() {
        if (!conversation) return;
        recognizer.startListening(speechIntent);
    }

    private void respond(String text) {
        String q = normalize(text);

        if (!q.startsWith("jarbas")) {
            status.setText("Modo mãos-livres ativo.");
            if (conversation) status.postDelayed(() -> listenAgain(), 300);
            return;
        }

        q = q.replace("jarbas", "").trim();
        if (q.isEmpty()) {
            status.setText("Modo mãos-livres ativo.");
            if (conversation) status.postDelayed(() -> listenAgain(), 300);
            return;
        }
        String answer;

        if (q.contains("quem e voce") || q.contains("seu nome")) {
            answer = "Eu sou JARBAS, o assistente do seu Gol 1999.";
        } else if (q.contains("como esta") || q.contains("estado do carro") || q.equals("carro")) {
            answer = "O módulo de diagnóstico ainda está em modo de demonstração. A próxima etapa conecta os sensores reais do Gol.";
        } else if (q.contains("temperatura")) {
            answer = "A leitura real da temperatura será fornecida pelo controlador veicular. Neste pacote ela ainda está em modo de demonstração.";
        } else if (q.contains("bateria")) {
            answer = "A tensão da bateria será lida pelo módulo veicular. O aplicativo já está preparado para receber essa informação.";
        } else if (hasAny(q, "aumentar volume", "aumenta volume", "aumente volume", "subir volume", "sobe volume", "mais alto", "aumentar som", "aumente o som")) {
            changeVolume(AudioManager.ADJUST_RAISE);
            answer = "Aumentando o volume.";
        } else if (hasAny(q, "diminuir volume", "diminui volume", "diminua volume", "abaixar volume", "abaixa volume", "baixar volume", "baixa volume", "mais baixo", "diminuir som", "abaixar som")) {
            changeVolume(AudioManager.ADJUST_LOWER);
            answer = "Diminuindo o volume.";
        } else if (hasAny(q, "volume maximo", "som maximo")) {
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC,
                    audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC), 0);
            answer = "Volume máximo selecionado.";
        } else if (hasAny(q, "volume minimo", "som minimo", "silenciar", "tirar o som", "mudo")) {
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0);
            answer = "Som silenciado.";
        } else if (hasAny(q, "pausar musica", "pausa musica", "pare a musica", "parar musica", "pause")) {
            sendMediaKey(KeyEvent.KEYCODE_MEDIA_PAUSE);
            answer = "Música pausada.";
        } else if (hasAny(q, "continuar musica", "continua musica", "retomar musica", "retome musica")) {
            sendMediaKey(KeyEvent.KEYCODE_MEDIA_PLAY);
            answer = "Continuando a música.";
        } else if (hasAny(q, "proxima musica", "proxima faixa", "proxima", "seguinte", "trocar musica", "mudar musica", "troca musica", "muda musica")) {
            sendMediaKey(KeyEvent.KEYCODE_MEDIA_NEXT);
            answer = "Avançando para a próxima música.";
        } else if (hasAny(q, "musica anterior", "faixa anterior", "voltar musica", "anterior")) {
            sendMediaKey(KeyEvent.KEYCODE_MEDIA_PREVIOUS);
            answer = "Voltando para a música anterior.";
        } else if (hasAny(q, "tocar musica", "tocar", "iniciar musica", "inicia musica", "dar play", "play")) {
            sendMediaKey(KeyEvent.KEYCODE_MEDIA_PLAY);
            answer = "Iniciando a música.";
        } else if (hasAny(q, "bluetooth", "conectar na central", "conectar o carro")) {
            openBluetoothSettings();
            answer = "Abrindo as configurações Bluetooth para você conectar a CAR-KIT.";
        } else if (hasAny(q, "obrigado", "obrigada")) {
            answer = "Sempre às ordens, Carlos.";
        } else {
            answer = "Entendi, Carlos. Ainda estou aprendendo os comandos específicos do seu Gol.";
        }

        status.setText("JARBAS: " + answer);
        if (!isQuietMediaCommand(q)) say(answer);
        if (conversation) status.postDelayed(() -> listenAgain(), 1200);
    }

    private boolean isQuietMediaCommand(String text) {
        return hasAny(text, "volume", "som", "musica", "faixa", "tocar", "play",
                "pausar", "pausa", "continuar", "retomar", "proxima", "anterior",
                "bluetooth", "conectar");
    }

    private String normalize(String text) {
        return Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9 ]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private boolean hasAny(String text, String... options) {
        for (String option : options) {
            if (text.contains(option)) return true;
        }
        return false;
    }

    private void say(String text) {
        status.setText("JARBAS: " + text);
        if (tts != null) tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "JARBAS");
    }

    private void changeVolume(int direction) {
        if (audioManager != null) {
            audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, direction, 0);
        }
    }

    private void sendMediaKey(int keyCode) {
        if (audioManager == null) return;
        long eventTime = System.currentTimeMillis();
        audioManager.dispatchMediaKeyEvent(new KeyEvent(eventTime, eventTime,
                KeyEvent.ACTION_DOWN, keyCode, 0));
        audioManager.dispatchMediaKeyEvent(new KeyEvent(eventTime, eventTime,
                KeyEvent.ACTION_UP, keyCode, 0));
    }

    private void openBluetoothSettings() {
        startActivity(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS));
    }

    @Override public void onInit(int result) {
        if (result == TextToSpeech.SUCCESS) {
            Locale portugueseBrazil = new Locale("pt", "BR");
            tts.setLanguage(portugueseBrazil);
            tts.setVoice(findMalePortugueseVoice(portugueseBrazil));
            tts.setPitch(0.88f);
            tts.setSpeechRate(0.90f);
        }
    }

    private Voice findMalePortugueseVoice(Locale locale) {
        Voice fallback = tts.getVoice();
        for (Voice voice : tts.getVoices()) {
            String name = voice.getName().toLowerCase(Locale.ROOT);
            if (voice.getLocale().equals(locale) &&
                    (name.contains("male") || name.contains("masculine") || name.contains("homem"))) {
                return voice;
            }
        }
        return fallback;
    }

    @Override protected void onDestroy() {
        conversation = false;
        if (recognizer != null) recognizer.destroy();
        if (tts != null) { tts.stop(); tts.shutdown(); }
        super.onDestroy();
    }
}
