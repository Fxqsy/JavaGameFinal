package entities;

import static utilz.Constants.*;
import static utilz.Constants.PlayerConstants.*;
import static utilz.HelpMethods.*;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

import Objects.Projectile;
import gamestates.Playing;
import main.Game;
import utilz.LoadSave;

public class Player extends Entity {
	private BufferedImage[][] animations;

	private boolean moving = false, attacking = false;
	private boolean left, right, jump;
	private int[][] lvlData;
	private float xDrawOffset = 9 * Game.SCALE;
	private float yDrawOffSet = 11 * Game.SCALE;

	// Jumping / Gravity
	private float jumpSpeed = -2.50f * Game.SCALE;
	private float fallSpeedAfterCollision = 0.5f * Game.SCALE;


	//status bar
	private BufferedImage statusBarImg;

	private int statusBarWidth = (int) (192 * Game.SCALE);
	private int statusBarHeight = (int) (58 * Game.SCALE);
	private int statusBarX = (int) (10 * Game.SCALE);
	private int statusBarY = (int) (380 * Game.SCALE);

	private int healthBarWidth = (int) (150 * Game.SCALE);
	private int healthBarHeight = (int) (4 * Game.SCALE);
	private int healthBarXStart = (int) (34 * Game.SCALE);
	private int healthBarYStart = (int) (14 * Game.SCALE);

	private boolean facingRight = true;

	private int healthWidth = healthBarWidth;


	private int flipX = 0;
	private int flipW = 1;

	private boolean jumpBoostActive = false;
	private long jumpBoostEndTime = 0;
	private boolean attackChecked;

	private Playing playing;

	private int tileY=0;

	private ArrayList<Projectile> spells = new ArrayList<>();



	public Player(float x, float y, int width, int height, Playing playing) {
		super(x, y, width, height);
		this.playing = playing;
		this.state = IDLE;
		this.maxHealth = 1000;
		this.currentHealth = maxHealth;
		this.walkSpeed = Game.SCALE * 1.0f;
		loadAnimations();
		initHitbox(16,26);
		initAttackBox();

	}

	public void setSpawn(Point spawn){
		this.x = spawn.x;
		this.y = spawn.y;
		hitbox.x = x;
		hitbox.y = y;
	}

	private void initAttackBox() {
		attackBox = new Rectangle2D.Float(x,y,(int) (20*Game.SCALE) , (int) (30*Game.SCALE));
	}

	public void update() {
		updateHealthBar();

		if(currentHealth <= 0){
			playing.setGameOver(true);
			return;
		}
		if (jumpBoostActive && System.currentTimeMillis() > jumpBoostEndTime) {
			jumpBoostActive = false;
		}


		updateAttackBox();

		updatePos();
		if(moving) {
			checkPotionTouched();
			checkSpikesTouched();
			tileY = (int)(hitbox.y / Game.TILES_SIZE);
		}
		if(attacking)
			checkAttack();
		updateAnimationTick();
		setAnimation();
	}

	public void shootProjectile() {
		int dir = isFacingRight() ? 1 : -1;
		int projectileX =(int) (hitbox.x + (dir == 1 ? hitbox.width / 2 -25 : -hitbox.width / 2));
		int projectileY = (int) (hitbox.y + height / 3);
		spells.add(new Projectile(projectileX, projectileY, dir));
	}

	private void checkSpikesTouched() {
		playing.checkSpikesTouched(this);
	}

	private void checkPotionTouched() {
		playing.checkPotionTouched(hitbox);
	}

	private void checkAttack() {
		if(attackChecked || aniIndex!=4)
			return;
		attackChecked = true;
		playing.checkEnemyHit(attackBox);
		playing.checkObjectHit(attackBox);

	}

	public void updateSpells() {
		for (Projectile s : spells) {
			if (s.isActive()) {
				s.updatePos();
			}
		}
	}

	public void drawSpell(Graphics g, int xLvlOffset) {

		ArrayList<Projectile> spellsToDraw = new ArrayList<>(spells);

		for (Projectile p : spellsToDraw) {
			if (p.isActive()) {
				g.drawImage(LoadSave.GetSpriteAtlas(LoadSave.CANNON_BALL),
						(int) (p.getHitbox().x - xLvlOffset),
						(int) (p.getHitbox().y -30),
						100, 100, null);
			}
		}
	}

	private void updateAttackBox() {
		if(right){
			attackBox.x = hitbox.x + hitbox.width + (int)(Game.SCALE * 2);
		}else if(left){
			attackBox.x = hitbox.x - hitbox.width - (int)(Game.SCALE * 4);
		}

		attackBox.y = hitbox.y + (Game.SCALE * 1);
	}

	private void updateHealthBar() {
		healthWidth = (int) (healthBarWidth * currentHealth / (float)( maxHealth));
	}

	public void render(Graphics g, int lvlOffset) {
		g.drawImage(animations[state][aniIndex],
				(int) (hitbox.x - xDrawOffset) - lvlOffset + flipX ,
				(int) (hitbox.y - yDrawOffSet),
				width * flipW, height, null);
		drawHitbox(g, lvlOffset);
		drawAttackBox(g,lvlOffset);

		drawUI(g);
	}



	private void drawUI(Graphics g) {
		g.drawImage(statusBarImg, statusBarX, statusBarY, statusBarWidth, statusBarHeight, null);
		g.setColor(Color.RED);
		g.fillRect(healthBarXStart + statusBarX, healthBarYStart + statusBarY, healthWidth, healthBarHeight);
	}

	private void updateAnimationTick() {
		aniTick++;
		if (aniTick >= ANI_SPEED_CHAR) {
			aniTick = 0;
			aniIndex++;
			if (aniIndex >= GetSpriteAmount(state)) {
				aniIndex = 0;
				attacking = false;
				attackChecked = false;
			}

		}

	}

	private void setAnimation() {
		int startAni = state;

		if (moving)
			state = RUNNING;
		else
			state = IDLE;

		if (inAir) {
			if (airSpeed < 0)
				state = JUMP;
			else
				state = FALLING;
		}

		if (attacking){
			state = ATTACK_1;
			if(startAni != ATTACK_1){
				aniIndex = 3;
				aniTick = 0;
				return;
			}
		}


		if (startAni != state)
			resetAniTick();
	}

	private void resetAniTick() {
		aniTick = 0;
		aniIndex = 0;
	}

	private void updatePos() {
		moving = false;

		if (jump)
			jump();

		if (!inAir)
			if ((!left && !right) || (right && left))
				return;

		float xSpeed = 0;

		if (left){
			xSpeed -= walkSpeed;
			flipX = width;
			flipW = -1;
		}

		if (right){
			xSpeed += walkSpeed;
			flipX = 0;
			flipW = 1;
		}

		if (!inAir)
			if (!IsEntityOnFloor(hitbox, lvlData))
				inAir = true;

		if (inAir) {
			if (CanMoveHere(hitbox.x, hitbox.y + airSpeed, hitbox.width, hitbox.height, lvlData)) {
				hitbox.y += airSpeed;
				airSpeed += GRAVITY;
				updateXPos(xSpeed);
			} else {
				hitbox.y = GetEntityYPosUnderRoofOrAboveFloor(hitbox, airSpeed);
				if (airSpeed > 0)
					resetInAir();
				else
					airSpeed = fallSpeedAfterCollision;
				updateXPos(xSpeed);
			}

		} else
			updateXPos(xSpeed);
		moving = true;
	}

	private void jump() {
		if (inAir)
			return;
		inAir = true;
		if(!jumpBoostActive)
			airSpeed = jumpSpeed;
		else
			airSpeed = BOOSTED_JUMP;

	}

	private void resetInAir() {
		inAir = false;
		airSpeed = 0;

	}

	private void updateXPos(float xSpeed) {
		if (CanMoveHere(hitbox.x + xSpeed, hitbox.y, hitbox.width, hitbox.height, lvlData)) {
			hitbox.x += xSpeed;
		} else {
			hitbox.x = GetEntityXPosNextToWall(hitbox, xSpeed);
		}
	}

	public void changeHealth(int value) {
		currentHealth+= value;
		if(currentHealth <= 0)
		{
			currentHealth = 0;
			//gameover
		}else if(currentHealth >= maxHealth){
			currentHealth = maxHealth;
		}
	}

	public void changePower(int value){
		System.out.println("POWER");
	}

	private void loadAnimations() {

		BufferedImage img = LoadSave.GetSpriteAtlas(LoadSave.PLAYER_ATLAS);

		animations = new BufferedImage[8][8];
		for (int j = 0; j < animations.length; j++)
			for (int i = 0; i < animations[j].length; i++)
				animations[j][i] = img.getSubimage(i * 32, j * 32, 32, 32);

		statusBarImg = LoadSave.GetSpriteAtlas(LoadSave.STATUS_BAR);
	}

	public void loadLvlData(int[][] lvlData) {
		this.lvlData = lvlData;
		if (!IsEntityOnFloor(hitbox, lvlData))
			inAir = true;
		spells.clear();

	}
	public void activateJumpBoost(int durationSeconds) {
		jumpBoostActive = true;
		jumpBoostEndTime = System.currentTimeMillis() + (durationSeconds * 1000);
	}


	public void resetDirBooleans() {
		left = false;
		right = false;
	}

	public void setAttacking(boolean attacking) {
		this.attacking = attacking;
	}

	public void setLeft(boolean left) {
		this.left = left;
	}

	public void setRight(boolean right) {
		this.right = right;
	}

	public boolean isFacingRight(){
		return facingRight;
	}



	public void setJump(boolean jump) {
		this.jump = jump;
	}

	public void resetAll() {
		resetDirBooleans();
		inAir = false;
		attacking = false;
		moving = false;
		state = IDLE;
		currentHealth = maxHealth;

		hitbox.x = x;
		hitbox.y = y;
		if(!IsEntityOnFloor(hitbox,lvlData))
			inAir = true;
	}

	public void kill() {
		currentHealth = 0;
	}

	public int getTileY(){
		return tileY;
	}

	public ArrayList<Projectile> getSpells() {
		return spells;
	}

	public void setFacingRight(boolean b) {
		this.facingRight = b;
	}

	public void clearSpells() {
		spells.clear();
	}
}