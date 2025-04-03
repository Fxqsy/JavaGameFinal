package entities;

import gamestates.Playing;
import levels.Level;
import utilz.LoadSave;
import static utilz.Constants.EnemyConstants.*;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

public class EnemyManager {

    private  Playing playing;
    private BufferedImage[][] skellyArr;
    private ArrayList<Skelly> skellies = new ArrayList<>();

    public EnemyManager(Playing playing) {
        this.playing = playing;
        loadEnemyImgs();
    }

    public void loadEnemies(Level level) {
        skellies = level.getSkellies();

    }

    public void update(int[][] lvlData, Player player){
        boolean isAnyActive = false;
        for(Skelly s : skellies) {
            if (s.isActive()) {
                s.update(lvlData, player);
                isAnyActive = true;
            }
        }
        if(!isAnyActive){
            playing.setLevelCompleted(true);
            }
    }

    public void draw(Graphics g, int xLvlOffset){
        drawSkellys(g,xLvlOffset);

        for(Skelly s : skellies) {
            s.drawHitbox(g, xLvlOffset);
        }

    }

    private void drawSkellys(Graphics g, int xLvlOffset) {
        for(Skelly s : skellies)
            if(s.isActive())
            {
                g.drawImage(skellyArr[s.getEnemyState()][s.getAniIndex()],
                        (int)(s.getHitbox().x - xLvlOffset- SKELLY_DRAWOFFSET_X) + s.flipX(),
                        (int)(s.getHitbox().y- SKELLY_DRAWOFFSET_Y),
                        SKELLY_WIDTH * s.flipW(),
                        SKELLY_HEIGHT,
                        null);
                s.drawHitbox(g, xLvlOffset);
                s.drawAttackBox(g, xLvlOffset);
            }
    }

    public void checkEnemyHit(Rectangle2D.Float attackBox){
        for(Skelly s: skellies)
            if(s.isActive())
                if(attackBox.intersects(s.getHitbox())){
                    s.hurt(10);
                    return;
                }


    }

    private void loadEnemyImgs() {
        skellyArr = new BufferedImage[5][11];
        BufferedImage temp = LoadSave.GetSpriteAtlas(LoadSave.SKELLY_SPRITE);
        for (int i = 0; i < skellyArr.length; i++)
            for (int j = 0; j < skellyArr[i].length; j++)
                skellyArr[i][j] = temp.getSubimage(j * SKELLY_WIDTH_DEFAULT, i * SKELLY_HEIGHT_DEFAULT, SKELLY_WIDTH_DEFAULT, SKELLY_HEIGHT_DEFAULT);
    }

    public void resetAllEnemies(){
        for(Skelly s : skellies)
            s.resetEnemy();
    }
}
