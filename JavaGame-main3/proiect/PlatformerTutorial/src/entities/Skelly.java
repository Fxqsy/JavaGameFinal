package entities;

import main.Game;
import java.awt.geom.Rectangle2D;
import static utilz.Constants.Directions.LEFT;
import static utilz.Constants.EnemyConstants.*;
import static utilz.HelpMethods.*;

public class Skelly extends Enemy {
    private int attackBoxOffsetX;
    private boolean dying = false;
    private boolean dead = false;

    public Skelly(float x, float y) {
        super(x, y, SKELLY_WIDTH, SKELLY_HEIGHT, SKELLY);
        initHitbox(17, 31);
        initAttackBox();
    }

    private void initAttackBox() {
        attackBox = new Rectangle2D.Float(x,y,(int)(30 * Game.SCALE),(int)(31 * Game.SCALE));
        attackBoxOffsetX=(int)(Game.SCALE*30);
    }

    public void update(int[][] lvlData, Player player) {
        if (dead || !active) return;  // Also check active status

        updateBehavior(lvlData, player);
        updateAniTick();
        updateAttackBox();

        // Check if death animation completed
        if (dying && aniIndex >= GetSpriteAmount(enemyType, state) - 1) {
            dead = true;
            active = false;  // Set to inactive when dead
        }
    }

    private void updateAttackBox() {
        if (walkDir == LEFT) {
            attackBox.x = hitbox.x - attackBoxOffsetX;
        } else {
            attackBox.x = hitbox.x + hitbox.width;
        }
        attackBox.y = hitbox.y;
    }

    private void updateBehavior(int[][] lvlData, Player player) {
        if (firstUpdate)
            firstUpdateCheck(lvlData);

        if (inAir) {
            updateInAir(lvlData);
        } else {
            switch(state) {
                case IDLE:
                    newState(RUNNING);
                    break;
                case RUNNING:
                    if (canSeePlayer(lvlData, player)) {
                        turnTowardsPlayer(player);
                        if (isPlayerCloseForAttack(player))
                            newState(ATTACK);
                    }
                    move(lvlData);
                    break;
                case ATTACK:
                    if (aniIndex == 0)
                        attackChecked = false;
                    if (aniIndex == 5 && !attackChecked)
                        checkEnemyHit(attackBox, player);
                    break;
                case HIT:
                    if (aniIndex >= GetSpriteAmount(enemyType, state) - 1) {
                        if (currentHealth <= 0) {
                            dying = true;
                            newState(DEAD);
                        } else {
                            newState(RUNNING);
                        }
                    }
                    break;
                case DEAD:
                    // Death animation is handled in update()
                    break;
            }
        }
    }

    @Override
    public void hurt(int amount) {
        if (dying || dead || !active) return;

        super.hurt(amount);
        if (currentHealth <= 0) {
            newState(HIT); // Will transition to DEAD after HIT animation
        } else {
            newState(HIT);
        }
    }

    public boolean isDead() {
        return dead;
    }

    public int flipX() {
        return walkDir == LEFT ? width : 0;
    }

    public int flipW() {
        return walkDir == LEFT ? -1 : 1;
    }

    @Override
    public void resetEnemy() {
        super.resetEnemy();
        dying = false;
        dead = false;
        active = true;  // Ensure enemy is active when reset
    }

    @Override
    public boolean isActive() {
        return active && !dead;  // Only active if not dead
    }
}