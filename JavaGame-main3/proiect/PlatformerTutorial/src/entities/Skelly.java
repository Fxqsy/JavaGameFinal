package entities;

import main.Game;

import java.awt.*;
import java.awt.geom.Rectangle2D;

import static utilz.Constants.Directions.LEFT;
import static utilz.Constants.EnemyConstants.*;
import static utilz.HelpMethods.*;

public class Skelly extends Enemy{

    private int attackBoxOffsetX;

    public Skelly(float x, float y) {
        super(x, y, SKELLY_WIDTH, SKELLY_HEIGHT, SKELLY);
        initHitbox(17, 31);
        initAttackBox();
    }

    private void initAttackBox() {
        attackBox = new Rectangle2D.Float(x,y,(int)(30 * Game.SCALE),(int)(31 * Game.SCALE));
        attackBoxOffsetX=(int)(Game.SCALE*30);
    }

    void update(int[][] lvlData,Player player){
        updateBehavior(lvlData,player);
        updateAniTick();
        updateAttackBox();

    }

    private void updateAttackBox(){
        if(walkDir == LEFT){
            attackBox.x = hitbox.x - attackBoxOffsetX;
        }else {
            attackBox.x = hitbox.x + hitbox.width;
        }
        attackBox.y = hitbox.y;
    }

    private void updateBehavior(int[][] lvlData, Player player){
        if (firstUpdate)
            firstUpdateCheck(lvlData);

        if(inAir){
            updateInAir(lvlData);
        }else{
            switch(state){
                case IDLE:
                    newState(RUNNING);
                    break;
                case RUNNING:
                    if(canSeePlayer(lvlData,player)){
                        turnTowardsPlayer(player);
                        if(isPlayerCloseForAttack(player))
                            newState(ATTACK);
                    }


                    move(lvlData);
                    break;
                case ATTACK:
                    if(aniIndex == 0)
                        attackChecked = false;
                    if(aniIndex == 5 && !attackChecked)
                        checkEnemyHit(attackBox,player);
                    break;
                case HIT:
                    break;
            }

        }

    }



    public int flipX(){
        if(walkDir == LEFT)
            return width;
        else
            return 0;
    }

    public int flipW(){
        if(walkDir == LEFT)
            return -1;
        else
            return 1;

    }

}
