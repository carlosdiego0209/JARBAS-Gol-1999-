package br.com.jarbas.gol;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
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
    private boolean conversation = false;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_main);

        status = findViewById(R.id.status);
        Button listen = findViewById(R.id.listen);
        Button stop = findViewById(R.id.stop);

        tts = new TextToSpeech(this, this);

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
        } else if (q.contains("música") || q.contains("musica")) {
            answer = "Posso conversar pelo áudio Bluetooth da central. O controle da fonte de música será integrado na próxima etapa.";
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

    @Override public void onInit(int result) {
        if (result == TextToSpeech.SUCCESS) {
            tts.setLanguage(new Locale("pt", "BR"));
            tts.setSpeechRate(0.95f);
        }
    }

    @Override protected void onDestroy() {
        conversation = false;
        if (recognizer != null) recognizer.destroy();
        if (tts != null) { tts.stop(); tts.shutdown(); }
        super.onDestroy();
    }
}
