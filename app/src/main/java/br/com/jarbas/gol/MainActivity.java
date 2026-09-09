package br.com.jarbas.gol;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.SharedPreferences;
import android.net.Uri;
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
    private int previousMusicVolume = -1;
    private boolean conversation = false;
    private boolean silentMode = false;
    private SharedPreferences memory;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_main);

        status = findViewById(R.id.status);
        Button listen = findViewById(R.id.listen);
        Button stop = findViewById(R.id.stop);

        tts = new TextToSpeech(this, this);
        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        memory = getSharedPreferences("jarb_memory", MODE_PRIVATE);

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
            public void onReadyForSpeech(Bundle p) { status.setText("JARB ouvindo."); }
            public void onBeginningOfSpeech() { status.setText("..."); }
            public void onRmsChanged(float r) {}
            public void onBufferReceived(byte[] b) {}
            public void onEndOfSpeech() { status.setText("Processando..."); }
            public void onError(int e) {
                restoreMusicVolume();
                conversation = false;
                status.setText("Toque em OUVIR JARB para falar.");
            }
            public void onResults(Bundle r) {
                restoreMusicVolume();
                conversation = false;
                ArrayList<String> a = r.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                String heard = (a != null && !a.isEmpty()) ? a.get(0) : "";
                respond(heard);
            }
            public void onPartialResults(Bundle r) {}
            public void onEvent(int t, Bundle p) {}
        });

        listen.setOnClickListener(v -> {
            silentMode = false;
            conversation = true;
            status.setText("Fale um comando começando com JARB.");
            listenAgain();
        });

        stop.setOnClickListener(v -> {
            conversation = false;
            silentMode = false;
            recognizer.cancel();
            restoreMusicVolume();
            status.setText("JARB em espera.");
        });
    }

    private void listenAgain() {
        if (!conversation) return;
        lowerMusicVolume();
        recognizer.startListening(speechIntent);
    }

    private void lowerMusicVolume() {
        if (audioManager == null || previousMusicVolume >= 0) return;
        previousMusicVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        int reducedVolume = Math.max(0, previousMusicVolume - 2);
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, reducedVolume, 0);
    }

    private void restoreMusicVolume() {
        if (audioManager == null || previousMusicVolume < 0) return;
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, previousMusicVolume, 0);
        previousMusicVolume = -1;
    }

    private void respond(String text) {
        String q = normalize(text);
        q = removeOptionalAssistantPrefix(q);

        if (startsWithAssistantName(q)) {
            q = q.replaceFirst("^jarb(?:as|s)?\\s*", "").trim();
        }

        if (hasAny(q, "silencio", "fique em silencio", "fica em silencio", "modo silencio", "pare de ouvir",
            "parar de ouvir", "desligue o microfone", "desativar escuta")) {
            silentMode = true;
            conversation = false;
            recognizer.cancel();
            status.setText("JARB em silêncio. Toque em OUVIR para reativar.");
            return;
        }

        if (q.isEmpty()) {
            silentMode = false;
            conversation = true;
            status.setText("JARB ativo. Diga o comando.");
            if (conversation) status.postDelayed(() -> listenAgain(), 300);
            return;
        }

        if (silentMode) {
            status.setText("JARB em silêncio. Toque em OUVIR para reativar.");
            return;
        }

        if ((q.startsWith("aprenda que ") || q.startsWith("aprenda ") || q.startsWith("me ensine que ")) && q.contains(" significa ")) {
            String lessonText = q.replaceFirst("^(aprenda que|aprenda|me ensine que)\\s+", "");
            String[] lesson = lessonText.split(" significa ", 2);
            if (lesson.length == 2 && !lesson[0].trim().isEmpty() && !lesson[1].trim().isEmpty()) {
                memory.edit().putString("alias_" + lesson[0].trim(), lesson[1].trim()).apply();
                answerAfterCommand("JARB aprendeu esse comando.");
                return;
            }
        }

        q = applyLearnedAliases(q);

        String answer;

        if (q.startsWith("me chame de ") || q.startsWith("me chama de ")) {
            String preferredName = q.substring(q.indexOf(" de ") + 4).trim();
            memory.edit().putString("preferred_name", preferredName).apply();
            answer = "Combinado. Vou chamar você de " + preferredName + ".";
        } else if (hasAny(q, "quem e voce", "quem e o jarb", "seu nome", "como voce se chama")) {
            answer = "Eu sou JARB, o assistente do seu Gol 1999.";
        } else if (hasAny(q, "bom dia", "boa tarde", "boa noite", "ola", "oi")) {
            answer = greetingForTime();
        } else if (hasAny(q, "o que voce sabe fazer", "ajuda", "comandos", "o que voce consegue", "me ajude")) {
            answer = "Posso conversar, pesquisar na internet, controlar volume e música, abrir o Bluetooth e guardar preferências que você me ensinar.";
        } else if (hasAny(q, "aumentar volume", "aumentar o volume", "aumenta volume", "aumenta o volume", "aumente volume", "aumente o volume", "subir volume", "subir o volume", "sobe volume", "sobe o volume", "mais alto", "aumentar som", "aumentar o som", "aumenta o som", "aumente o som", "volume pra cima")) {
            changeVolume(AudioManager.ADJUST_RAISE);
            answer = "Aumentando o volume.";
        } else if (hasAny(q, "diminuir volume", "diminuir o volume", "diminui volume", "diminui o volume", "diminua volume", "diminua o volume", "abaixar volume", "abaixar o volume", "abaixa volume", "abaixa o volume", "baixar volume", "baixar o volume", "baixa volume", "baixa o volume", "mais baixo", "diminuir som", "diminuir o som", "abaixar som", "abaixar o som", "abaixa o som", "volume pra baixo")) {
            changeVolume(AudioManager.ADJUST_LOWER);
            answer = "Diminuindo o volume.";
        } else if (hasAny(q, "volume maximo", "som maximo", "no maximo")) {
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC,
                    audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC), 0);
            answer = "Volume máximo selecionado.";
        } else if (hasAny(q, "volume minimo", "som minimo", "silenciar", "tirar o som", "sem som", "mudo")) {
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0);
            answer = "Som silenciado.";
        } else if (hasAny(q, "abrir spotify", "abre o spotify", "abrir o spotify", "spotify", "tocar spotify")) {
            openSpotify();
            answer = "Abrindo o Spotify.";
        } else if (hasAny(q, "pausar musica", "pausar a musica", "pausa musica", "pausa a musica", "pause a musica", "coloca em pausa", "pare a musica", "parar musica", "pause")) {
            sendMediaKey(KeyEvent.KEYCODE_MEDIA_PAUSE);
            answer = "Enviando comando para pausar a música.";
        } else if (hasAny(q, "continuar musica", "continuar a musica", "continua musica", "continua a musica", "retomar musica", "retome musica", "voltar a tocar", "da play")) {
            sendMediaKey(KeyEvent.KEYCODE_MEDIA_PLAY);
            answer = "Enviando comando para continuar a música.";
        } else if (hasAny(q, "proxima musica", "proxima a musica", "proxima faixa", "proxima", "seguinte", "trocar musica", "mudar musica", "troca musica", "muda musica", "pular musica", "pula musica")) {
            sendMediaKey(KeyEvent.KEYCODE_MEDIA_NEXT);
            answer = "Enviando comando para a próxima música.";
        } else if (hasAny(q, "musica anterior", "faixa anterior", "voltar musica", "voltar faixa", "musica passada", "anterior")) {
            sendMediaKey(KeyEvent.KEYCODE_MEDIA_PREVIOUS);
            answer = "Enviando comando para a música anterior.";
        } else if (hasAny(q, "tocar musica", "tocar a musica", "tocar", "iniciar musica", "inicia musica", "dar play", "da play", "play", "reproduzir musica")) {
            sendMediaKey(KeyEvent.KEYCODE_MEDIA_PLAY);
            answer = "Iniciando a música.";
        } else if (hasAny(q, "pesquise", "pesquisar", "procure na internet", "buscar na internet", "veja na internet", "noticias sobre")) {
            String search = q.replaceFirst("^(pesquise|pesquisar|procure(?: na internet)?|buscar(?: na internet)?|veja na internet|noticias sobre|o que e|quem foi|como funciona|qual e)\\s*", "").trim();
            if (search.isEmpty()) search = q;
            openWebSearch(search);
            answer = "Abrindo uma pesquisa na internet.";
        } else if (hasAny(q, "como esta", "estado do carro", "situacao do carro", "status do carro", "carro")) {
            answer = "O módulo de diagnóstico ainda está em modo de demonstração. A próxima etapa conecta os sensores reais do Gol.";
        } else if (q.contains("temperatura")) {
            answer = "A leitura real da temperatura será fornecida pelo controlador veicular. Neste pacote ela ainda está em modo de demonstração.";
        } else if (q.contains("bateria")) {
            answer = "A tensão da bateria será lida pelo módulo veicular. O aplicativo já está preparado para receber essa informação.";
        } else if (hasAny(q, "bluetooth", "conectar na central", "conectar o carro", "conectar car kit", "abrir bluetooth")) {
            openBluetoothSettings();
            answer = "Abrindo as configurações Bluetooth para você conectar a CAR-KIT.";
        } else if (hasAny(q, "obrigado", "obrigada")) {
            answer = "Sempre às ordens, Carlos.";
        } else {
            answer = "Entendi. Posso pesquisar isso na internet se você disser: JARB, pesquise " + q + ".";
        }

        learnCommand(q);
        status.setText("JARB: " + answer);
        if (!silentMode) say(answer);
        conversation = false;
    }

    private void answerAfterCommand(String answer) {
        status.setText("JARB: " + answer);
        if (!silentMode) say(answer);
        conversation = false;
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
                .replaceAll("\\bpor favor\\b", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private boolean hasAny(String text, String... options) {
        for (String option : options) {
            if (text.contains(option)) return true;
        }
        return false;
    }

    private String removeOptionalAssistantPrefix(String text) {
        return text.replaceFirst("^(ia|assistente)\\s+", "").trim();
    }

    private boolean startsWithAssistantName(String text) {
        return text.matches("^jarb(?:as|s)?($|\\s+).*");
    }

    private String applyLearnedAliases(String text) {
        for (java.util.Map.Entry<String, ?> entry : memory.getAll().entrySet()) {
            if (!entry.getKey().startsWith("alias_")) continue;
            String alias = entry.getKey().substring("alias_".length());
            if (text.contains(alias)) return text.replace(alias, String.valueOf(entry.getValue()));
        }
        return text;
    }

    private void learnCommand(String command) {
        String key = "count_" + command;
        memory.edit().putInt(key, memory.getInt(key, 0) + 1).apply();
    }

    private void say(String text) {
        status.setText("JARB: " + text);
        if (tts != null) tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "JARB");
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

    private void openSpotify() {
        Intent spotify = getPackageManager().getLaunchIntentForPackage("com.spotify.music");
        if (spotify != null) {
            startActivity(spotify);
        } else {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://open.spotify.com/")));
        }
    }

    private void openWebSearch(String query) {
        String safeQuery = query.isEmpty() ? "Gol 1999" : query;
        startActivity(new Intent(Intent.ACTION_VIEW,
                Uri.parse("https://www.google.com/search?q=" + Uri.encode(safeQuery))));
    }

    private String greetingForTime() {
        java.util.Calendar now = java.util.Calendar.getInstance();
        int hour = now.get(java.util.Calendar.HOUR_OF_DAY);
        String greeting = hour < 12 ? "Bom dia" : hour < 18 ? "Boa tarde" : "Boa noite";
        String preferredName = memory.getString("preferred_name", "Carlos");
        return greeting + ", " + preferredName + ". Como posso ajudar?";
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
