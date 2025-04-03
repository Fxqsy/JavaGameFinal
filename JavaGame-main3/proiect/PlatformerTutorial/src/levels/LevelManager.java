package levels;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.nio.BufferOverflowException;
import java.util.ArrayList;

import audio.AudioPlayer;
import gamestates.Gamestate;
import gamestates.Menu;
import main.Game;
import utilz.LoadSave;

public class LevelManager {

	private Game game;
	private BufferedImage[] levelSprite;
	private ArrayList<Level> levels;
	private int lvlIndex = 0;

	public LevelManager(Game game) {
		this.game = game;
		importOutsideSprites();
		levels = new ArrayList<>();
		buildAllLevels();
	}

	public void loadNextLevel(){
		lvlIndex++;
		if(lvlIndex >= levels.size()){
			lvlIndex = 0; //daca vrei cutscene cand termini jocu aici vine
			System.out.println("no more levels");
			Gamestate.state = Gamestate.MENU;

		}

		Level newLevel = levels.get(lvlIndex);
		game.getPlaying().getEnemyManager().loadEnemies(newLevel);
		game.getPlaying().getPlayer().loadLvlData(newLevel.getLevelData());
		game.getPlaying().setMaxLvlOffset(newLevel.getLvlOffset());
		game.getPlaying().getObjectManager().loadObjects(newLevel);

	}

	private void buildAllLevels() {
		BufferedImage[] allLevels = LoadSave.GetAllLevels();
		for(BufferedImage img: allLevels)
			levels.add(new Level(img));
	}

	private void importOutsideSprites() {
		BufferedImage img = LoadSave.GetSpriteAtlas(LoadSave.LEVEL_ATLAS);
		levelSprite = new BufferedImage[96];
		for (int j = 0; j < 8; j++)
			for (int i = 0; i < 12; i++) {
				int index = j * 12 + i;
				levelSprite[index] = img.getSubimage(i * 32, j * 32, 32, 32);
			}
	}

	public void draw(Graphics g, int lvlOffset) {
		for (int j = 0; j < Game.TILES_IN_HEIGHT; j++)
			for (int i = 0; i < levels.get(lvlIndex).getLevelData()[0].length; i++) {
				int index = levels.get(lvlIndex).getSpriteIndex(i, j);
				g.drawImage(levelSprite[index], Game.TILES_SIZE * i - lvlOffset, Game.TILES_SIZE * j, Game.TILES_SIZE, Game.TILES_SIZE, null);
			}
	}


	public void update() {

	}

	public Level getCurrentLevel() {
		return levels.get(lvlIndex);
	}

	public int getAmountOfLevels(){
		return levels.size();
	}

	public int getLvlIndex(){
		return lvlIndex;
	}
}
