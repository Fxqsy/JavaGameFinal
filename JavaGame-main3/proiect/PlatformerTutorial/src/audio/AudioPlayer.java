package audio;

import javax.sound.sampled.*;
import java.io.IOException;
import java.net.URL;
import java.util.Random;

public class AudioPlayer {

    public static int MENU_1 = 0;
    public static int LEVEL_1 = 1;
    public static int LEVEL_2 = 2;

    public static int DIE = 0;
    public static int JUMP = 1;
    public static int GAMEOVER = 2;
    public static int LVL_COMPLETED = 3;
    public static int ATTACK_ONE = 4;
    public static int ATTACK_TWO = 5;
    public static int ATTACK_RANGED = 6;

    private Clip[] songs, effects;
    private int currentSongId;
    private float volume =1f;
    private boolean songMute, effectMute;
    private Random rand = new Random();

    public AudioPlayer(){

        loadSongs();
        loadEffects();
        playSong(MENU_1);
    }

    private void loadSongs(){
        String[] names = { "menu", "level1", "level2" };
        songs = new Clip[names.length];
        for (int i = 0; i < songs.length; i++)
            songs[i] = getClip(names[i]);
    }

    private void loadEffects(){
        String[] effectNames = { "die", "jump", "gameover", "lvlcompleted", "attack1", "attack2", "attack_range" };
        effects = new Clip[effectNames.length];
        for (int i = 0; i < effects.length; i++)
            effects[i] = getClip(effectNames[i]);

        updateEffectsVolume();

    }

    private Clip getClip(String name) {
        URL url = getClass().getResource("/audio/" + name + ".wav");
        if (url == null) {
            System.err.println("Audio file not found: " + name);
            return null;
        }

        try (AudioInputStream original = AudioSystem.getAudioInputStream(url)) {
            // Define target format (16-bit, 44.1kHz, stereo)
            AudioFormat targetFormat = new AudioFormat(
                    AudioFormat.Encoding.PCM_SIGNED,
                    44100,
                    16,
                    2,
                    4,  // frameSize
                    44100,
                    false);

            // Convert stream if needed
            AudioInputStream converted = AudioSystem.getAudioInputStream(targetFormat, original);
            Clip c = AudioSystem.getClip();
            c.open(converted);
            return c;
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void setVolume(float volume){
        this.volume = volume;
        updateSongVolume();
        updateEffectsVolume();
    }

    public void setLevelSong(int lvlIndex){
        if (lvlIndex % 2 == 0)
            playSong(LEVEL_1);
        else
            playSong(LEVEL_2);
    }

    public void lvlCompleted(){
        stopSong();
        playEffect(LVL_COMPLETED);
    }


    public void playAttackSound(){
        int start = 4;
        start += rand.nextInt(2);
        playEffect(start);
    }

    public void playAttackRanged(){
        playEffect(ATTACK_RANGED);
    }


    public void playEffect(int effect){
        effects[effect].setMicrosecondPosition(0);
        effects[effect].start();

    }

    public void playSong(int song){
        stopSong();

        currentSongId = song;
        updateSongVolume();
        songs[currentSongId].setMicrosecondPosition(0);
        songs[currentSongId].loop(Clip.LOOP_CONTINUOUSLY);

    }

    public void stopSong() {
        if (songs[currentSongId].isActive())
            songs[currentSongId].stop();
    }

    public void toggleSongMute(){
        this.songMute = !songMute;
        for(Clip c : songs){
            BooleanControl booleanControl = (BooleanControl) c.getControl(BooleanControl.Type.MUTE);
            booleanControl.setValue(songMute);
        }
    }

    public void toggleEffectMute(){
        this.effectMute = !effectMute;
        for (Clip c : effects) {
            BooleanControl booleanControl = (BooleanControl) c.getControl(BooleanControl.Type.MUTE);
            booleanControl.setValue(effectMute);
        }
        if (!effectMute)
            playEffect(ATTACK_RANGED);
    }

    private void updateSongVolume(){
        FloatControl gainControl = (FloatControl) songs[currentSongId].getControl(FloatControl.Type.MASTER_GAIN);
        float range = gainControl.getMaximum() - gainControl.getMinimum();
        float gain = (range * volume) + gainControl.getMinimum();
        gainControl.setValue(gain);

    }

    private void updateEffectsVolume(){
        for (Clip c : effects) {
            FloatControl gainControl = (FloatControl) c.getControl(FloatControl.Type.MASTER_GAIN);
            float range = gainControl.getMaximum() - gainControl.getMinimum();
            float gain = (range * volume) + gainControl.getMinimum();
            gainControl.setValue(gain);
        }
    }



}
