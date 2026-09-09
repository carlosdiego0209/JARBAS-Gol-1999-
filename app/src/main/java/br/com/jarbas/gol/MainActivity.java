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

        recognizer = SpeechRecognizer.createSpeechRecognizer(this);
        recognizer.setRecognitionListener(new RecognitionListener() {
            public void onReadyForSpeech(Bundle p) { status.setText("JARBAS está ouvindo..."); }
            public void onBeginningOfSpeech() { status.setText("Estou ouvindo, Carlos."); }
            public void onRmsChanged(float r) {}
            public void onBufferReceived(byte[] b) {}
            public void onEndOfSpeech() { status.setText("Processando..."); }
            public void onError(int e) {
                status.setText(conversation ? "Não entendi. Tentando novamente..." : "Toque em OUVIR para tentar novamente.");
                if (conversation) status.postDelayed(() -> listenAgain(), 900);
            }
            public void onResults(Bundle r) {
                ArrayList<String> a = r.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                String heard = (a != null && !a.isEmpty()) ? a.get(0) : "";
                status.setText("Carlos: " + heard);
                respond(heard);
            }
            public void onPartialResults(Bundle r) {}
            public void onEvent(int t, Bundle p) {}
        });

        listen.setOnClickListener(v -> {
            conversation = true;
            say("Sim, Carlos. JARBAS está pronto.");
            v.postDelayed(() -> listenAgain(), 1500);
        });

        stop.setOnClickListener(v -> {
            conversation = false;
            recognizer.cancel();
            status.setText("JARBAS em espera.");
            say("Até logo, Carlos.");
        });
    }

    private void listenAgain() {
        if (!conversation) return;
        recognizer.startListening(speechIntent);
    }

    private void respond(String text) {
        String q = text.toLowerCase(Locale.ROOT);
        String answer;

        if (q.contains("quem é você") || q.contains("seu nome")) {
            answer = "Eu sou JARBAS, o assistente do seu Gol 1999.";
        } else if (q.contains("como está") || q.contains("estado do carro") || q.contains("carro")) {
            answer = "O módulo de diagnóstico ainda está em modo de demonstração. A próxima etapa conecta os sensores reais do Gol.";
        } else if (q.contains("temperatura")) {
            answer = "A leitura real da temperatura será fornecida pelo controlador veicular. Neste pacote ela ainda está em modo de demonstração.";
        } else if (q.contains("bateria")) {
            answer = "A tensão da bateria será lida pelo módulo veicular. O aplicativo já está preparado para receber essa informação.";
        } else if (q.contains("aumenta o volume") || q.contains("aumentar o volume") || q.contains("mais alto")) {
            changeVolume(AudioManager.ADJUST_RAISE);
            answer = "Aumentando o volume.";
        } else if (q.contains("diminui o volume") || q.contains("diminuir o volume") || q.contains("mais baixo")) {
            changeVolume(AudioManager.ADJUST_LOWER);
            answer = "Diminuindo o volume.";
        } else if (q.contains("volume máximo") || q.contains("volume maximo")) {
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC,
                    audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC), 0);
            answer = "Volume máximo selecionado.";
        } else if (q.contains("volume mínimo") || q.contains("volume minimo") || q.contains("silenciar")) {
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0);
            answer = "Som silenciado.";
        } else if (q.contains("pausa a música") || q.contains("pausar a música") || q.contains("pausa a musica") || q.contains("pausar a musica")) {
            sendMediaKey(KeyEvent.KEYCODE_MEDIA_PAUSE);
            answer = "Música pausada.";
        } else if (q.contains("continua a música") || q.contains("continuar a música") || q.contains("continua a musica") || q.contains("continuar a musica")) {
            sendMediaKey(KeyEvent.KEYCODE_MEDIA_PLAY);
            answer = "Continuando a música.";
        } else if (q.contains("próxima música") || q.contains("proxima musica") || q.contains("próxima faixa") || q.contains("proxima faixa")) {
            sendMediaKey(KeyEvent.KEYCODE_MEDIA_NEXT);
            answer = "Avançando para a próxima música.";
        } else if (q.contains("música anterior") || q.contains("musica anterior") || q.contains("faixa anterior")) {
            sendMediaKey(KeyEvent.KEYCODE_MEDIA_PREVIOUS);
            answer = "Voltando para a música anterior.";
        } else if (q.equals("tocar música") || q.equals("tocar musica") || q.equals("tocar")) {
            sendMediaKey(KeyEvent.KEYCODE_MEDIA_PLAY);
            answer = "Iniciando a música.";
        } else if (q.contains("bluetooth") || q.contains("conectar na central") || q.contains("conectar o carro")) {
            openBluetoothSettings();
            answer = "Abrindo as configurações Bluetooth para você conectar a CAR-KIT.";
        } else if (q.contains("obrigado") || q.contains("obrigada")) {
            answer = "Sempre às ordens, Carlos.";
        } else {
            answer = "Entendi, Carlos. Ainda estou aprendendo os comandos específicos do seu Gol.";
        }

        say(answer);
        if (conversation) status.postDelayed(() -> listenAgain(), Math.max(1800, answer.length() * 55));
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
