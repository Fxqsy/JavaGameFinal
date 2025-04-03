package gamestates;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Random;

import Objects.ObjectManager;
import Objects.Projectile;
import entities.Enemy;
import entities.EnemyManager;
import entities.Player;
import levels.LevelManager;
import main.Game;
import ui.GameOverOverlay;
import ui.LevelCompletedOverlay;
import ui.PauseOverlay;
import utilz.LoadSave;
import static utilz.Constants.Environment.*;

public class Playing extends State implements Statemethods {
	private Player player;
	private Enemy enemy;
	private LevelManager levelManager;
	private EnemyManager enemyManager;
	private ObjectManager objectManager;
	private PauseOverlay pauseOverlay;
	private GameOverOverlay gameOverOverlay;
	private LevelCompletedOverlay levelCompletedOverlay;
	private boolean paused = false;

	private int xLvlOffset;
	private int leftBorder = (int) (0.2 * Game.GAME_WIDTH);
	private int rightBorder = (int) (0.8 * Game.GAME_WIDTH);
	private int maxLvlOffsetX;

	private BufferedImage backgroundImg, cloudImg, smallCloudImg;
	private int[] smallCloudsPos;
	private Random rnd = new Random();

	private boolean gameOver = false;
	private boolean lvlCompleted =false;

	private boolean playerDying = false;

	private float currentZoom = 1.0f;
	private float targetZoom = 1.0f;
	private final float ZOOM_SPEED = 0.05f;
	private final float DEATH_ZOOM_LEVEL = 4.0f;

	public Playing(Game game) {
		super(game);
		initClasses();

		backgroundImg = LoadSave.GetSpriteAtlas(LoadSave.PLAYING_BG_IMG);
		smallCloudImg = LoadSave.GetSpriteAtlas(LoadSave.SMALL_CLOUD);
		smallCloudsPos = new int[8];
		for(int i=0;i<smallCloudsPos.length;i++)
			smallCloudsPos[i] =(int)(40* Game.SCALE) + rnd.nextInt((int)(100*Game.SCALE));

		calcLvlOffset();
		loadStartLevel();
	}

	public void loadNextLevel(){
		resetAll();
		levelManager.loadNextLevel();
		player.setSpawn(levelManager.getCurrentLevel().getPlayerSpawn());
	}

	private void loadStartLevel() {
		enemyManager.loadEnemies(levelManager.getCurrentLevel());
		objectManager.loadObjects(levelManager.getCurrentLevel());
	}

	private void calcLvlOffset() {
		maxLvlOffsetX = levelManager.getCurrentLevel().getLvlOffset();
	}

	private void initClasses() {
		levelManager = new LevelManager(game);
		enemyManager = new EnemyManager(this);
		objectManager = new ObjectManager(this);

		player = new Player(200, 200, (int) (40 * Game.SCALE), (int) (40* Game.SCALE), this);
		player.loadLvlData(levelManager.getCurrentLevel().getLevelData());
		player.setSpawn(levelManager.getCurrentLevel().getPlayerSpawn());
		pauseOverlay = new PauseOverlay(this);
		gameOverOverlay = new GameOverOverlay(this);
		levelCompletedOverlay = new LevelCompletedOverlay(this);

	}

	@Override
	public void update() {
		if(paused){
			pauseOverlay.update();
		}
		else if(lvlCompleted){
			levelCompletedOverlay.update();
		}else if(gameOver){
			gameOverOverlay.update();
		}
		else if(playerDying){
			targetZoom = DEATH_ZOOM_LEVEL;
			player.update();
		}
		else{
			targetZoom = 1.0f;
			levelManager.update();
			objectManager.update(levelManager.getCurrentLevel().getLevelData(), player);
			player.update();
			player.updateSpells();
			enemyManager.update(levelManager.getCurrentLevel().getLevelData(), player);
			objectManager.updateSpells(levelManager.getCurrentLevel().getLevelData());

			checkClosetoBorder();
		}
		if (Math.abs(currentZoom - targetZoom) > 0.01f) {
			currentZoom += (targetZoom - currentZoom) * ZOOM_SPEED;
		} else {
			currentZoom = targetZoom;
		}

	}

	private void checkClosetoBorder() {
		int playerX = (int) player.getHitbox().x;
		int diff = playerX - xLvlOffset;

		if (diff > rightBorder)
			xLvlOffset += diff - rightBorder;
		else if (diff < leftBorder)
			xLvlOffset += diff - leftBorder;

		if (xLvlOffset > maxLvlOffsetX)
			xLvlOffset = maxLvlOffsetX;
		else if (xLvlOffset < 0)
			xLvlOffset = 0;

	}

	@Override
	public void draw(Graphics g) {
		Graphics2D g2d = (Graphics2D) g;
		AffineTransform originalTransform = g2d.getTransform();

		// Apply zoom transformation centered on player
		if (currentZoom != 1.0f) {
			int playerScreenX = (int)(player.getHitbox().x - xLvlOffset);
			int playerScreenY = (int)player.getHitbox().y;

			g2d.translate(playerScreenX, playerScreenY);
			g2d.scale(currentZoom, currentZoom);
			g2d.translate(-playerScreenX, -playerScreenY);
		}

		g.drawImage(backgroundImg, 0, 0, Game.GAME_WIDTH, Game.GAME_HEIGHT, null);

		drawClouds(g);

		levelManager.draw(g, xLvlOffset);
		player.render(g, xLvlOffset);
		enemyManager.draw(g, xLvlOffset);
		objectManager.draw(g,xLvlOffset);
		player.drawSpell(g,xLvlOffset);

		g2d.setTransform(originalTransform);

		if(paused){
			g.setColor(new Color(0,0,0,200));
			g.fillRect(0, 0, Game.GAME_WIDTH, Game.GAME_HEIGHT);
			pauseOverlay.draw(g);
		}
		else if(gameOver){
			gameOverOverlay.draw(g);
		}else if(lvlCompleted)
			levelCompletedOverlay.draw(g);


		g2d.setTransform(originalTransform);
	}

	private void drawClouds(Graphics g) {
//		for(int i=0;i<3;i++)
//			g.drawImage(cloudImg, i *CLOUD_WIDTH - (int)(xLvlOffset * 0.3), (int)(50* Game.SCALE), CLOUD_WIDTH, CLOUD_HEIGHT, null);
		for(int i=0;i<smallCloudsPos.length;i++)
			g.drawImage(smallCloudImg, SMALL_CLOUD_WIDTH * 4 * i- (int)(xLvlOffset * 0.7), smallCloudsPos[i], SMALL_CLOUD_WIDTH, SMALL_CLOUD_HEIGHT, null);
	}

	public void resetAll(){
		// reset player, enemy, lvl
		gameOver = false;
		paused = false;
		lvlCompleted = false;
		playerDying = false;
		player.resetAll();
		player.clearSpells();
		resetJumpBoost();
		enemyManager.resetAllEnemies();
		objectManager.resetAllObjects();

		currentZoom = 1.0f;
		targetZoom = 1.0f;
	}

	public void setGameOver(boolean gameOver){
		this.gameOver = gameOver;
	}

	public void checkEnemyHit(Rectangle2D.Float attackBox){
		enemyManager.checkEnemyHit(attackBox);
	}

	public void checkPotionTouched(Rectangle2D.Float hitbox){
		objectManager.checkObjectTouched(hitbox);
	}

	public void checkSpikesTouched(Player player) {
		objectManager.checkSpikesTouched(player);
	}
	@Override
	public void mouseClicked(MouseEvent e) {
		if(!gameOver)
			if (e.getButton() == MouseEvent.BUTTON1)
				player.setAttacking(true);
	}

	@Override
	public void keyPressed(KeyEvent e) {
		if(gameOver)
			gameOverOverlay.keyPressed(e);
		else
			switch (e.getKeyCode()) {
			case KeyEvent.VK_A:
				player.setFacingRight(false);
				player.setLeft(true);
				break;
			case KeyEvent.VK_D:
				player.setFacingRight(true);
				player.setRight(true);
				break;
			case KeyEvent.VK_W:
			case KeyEvent.VK_SPACE:
				player.setJump(true);
				break;
			case KeyEvent.VK_ESCAPE:
				paused = !paused;
				break;
			case KeyEvent.VK_M:
				if(!player.isShooting()){
					player.setShooting(true);
					player.shootProjectile();
				}

				break;
			}
	}

	@Override
	public void keyReleased(KeyEvent e) {
		if(!gameOver)
			switch (e.getKeyCode()) {
			case KeyEvent.VK_A:
				player.setLeft(false);
				break;
			case KeyEvent.VK_D:
				player.setRight(false);
				break;
			case KeyEvent.VK_W:
			case KeyEvent.VK_SPACE:
				player.setJump(false);
			case KeyEvent.VK_M:
				player.setShooting(false);
				break;
			}

	}

	public void mouseDragged(MouseEvent e) {
		if(!gameOver)
			if (paused)
				pauseOverlay.mouseDragged(e);
	}

	@Override
	public void mousePressed(MouseEvent e) {
		if(!gameOver) {
			if (paused)
				pauseOverlay.mousePressed(e);
			else if(lvlCompleted)
				levelCompletedOverlay.mousePressed(e);
		} else{
			gameOverOverlay.mousePressed(e);
		}
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		if (!gameOver) {
			if (paused)
				pauseOverlay.mouseReleased(e);
			 else if (lvlCompleted)
				levelCompletedOverlay.mouseReleased(e);
		} else{
			gameOverOverlay.mouseReleased(e);
		}
	}

	@Override
	public void mouseMoved(MouseEvent e) {
		if (!gameOver) {
			if (paused)
				pauseOverlay.mouseMoved(e);
			else if (lvlCompleted)
				levelCompletedOverlay.mouseMoved(e);
		}else {
			gameOverOverlay.mouseMoved(e);
			}
	}

	public void setLevelCompleted(boolean levelCompleted) {
		this.lvlCompleted = levelCompleted;
		if(levelCompleted)
			game.getAudioPlayer().lvlCompleted();
	}

	public void setMaxLvlOffset(int lvlOffset) {
		this.maxLvlOffsetX = lvlOffset;
	}

	public void unpauseGame(){
		paused = false;
	}

	public void windowFocusLost() {
		player.resetDirBooleans();
	}

	public Player getPlayer() {
		return player;
	}

	public EnemyManager getEnemyManager(){
		return enemyManager;
	}

	public ObjectManager getObjectManager(){
		return objectManager;
	}

	public void checkObjectHit(Rectangle2D.Float attackBox) {
		objectManager.checkObjectHit(attackBox);
	}

	public LevelManager getLevelManager(){
		return levelManager;
	}

	public void resetJumpBoost(){
		player.activateJumpBoost(0);
	}

	public void setPlayerDying(boolean playerDying) {
		this.playerDying = playerDying;
	}
}
