/**
 *   The MIT License
 *
 *  Copyright 2011 Andrew James <ephphatha@thelettereph.com>.
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 */
package au.edu.csu.bofsa;

import java.util.LinkedList;
import java.util.Queue;

import org.newdawn.slick.Graphics;
import org.newdawn.slick.geom.Rectangle;
import org.newdawn.slick.geom.Vector2f;

/**
 * @author ephphatha
 *
 */
public class Creep {
  protected Vector2f position,
                     goal,
                     checkpoint,
                     velocity;
  
  protected Queue<CheckPoint> checkpoints;
  
  protected Sprite sprite;
  protected Sprite.SequencePoint[] northSequence,
                                   southSequence,
                                   westSequence,
                                   eastSequence;

  protected Attributes attributes;
  
  public static class Attributes {
    public final Type type;
    
    public float hp,
                 value,
                 damage,
                 speed;

    Attributes(Type type) {
      this.type = type;
      
      this.hp = 0.0f;
      this.damage = 0.0f;
      this.speed = 1.0f;
    }
    
    Attributes(Type type, float hp, float damage, float speed) {
      this(type);
      
      this.setHealth(hp);
      
      this.setDamage(damage);
      
      this.setMaxSpeed(speed);
    }

    public void setMaxSpeed(float speed) {
      this.speed = speed;
    }

    public void setDamage(float damage) {
      this.damage = damage;
    }

    public void setHealth(float hp) {
      this.hp = hp;
    }
  }

  public enum Type {
    CUSTOMER,
    HOBO,
    AUDITOR
  }
  
  Creep(Sprite sprite, final Sprite.SequencePoint[] frames, final Vector2f position, final Queue<CheckPoint> checkpoints, final Vector2f goal, final Attributes attributes) {
    this.sprite = sprite;

    this.southSequence = new Sprite.SequencePoint[4];

    for (int i = 0; i < 4; ++i) {
      this.southSequence[i] = frames[i];
    }
    
    this.northSequence = new Sprite.SequencePoint[4];

    for (int i = 0; i < 4; ++i) {
      this.northSequence[i] = frames[i + 4];
    }
    
    this.westSequence = new Sprite.SequencePoint[4];

    for (int i = 0; i < 4; ++i) {
      this.westSequence[i] = frames[i + 8];
    }
    
    this.eastSequence = new Sprite.SequencePoint[4];
    
    for (int i = 0; i < 4; ++i) {
      this.eastSequence[i] = frames[i + 12];
    }
    
    this.position = position.copy();
    this.goal = goal;
    try {
      this.checkpoints = new LinkedList<CheckPoint>(checkpoints);
    } catch (NullPointerException e) {
      this.checkpoints= null;
    }
    this.attributes = attributes;
    
    this.calculateVelocity();
    
    this.getNextCheckpoint();
  }
  
  public void getNextCheckpoint() {
    if (this.checkpoints != null) {
      CheckPoint cp = this.checkpoints.poll();
      
      if (cp != null) {
        this.checkpoint = cp.position;
        return;
      }
    }
    
    this.checkpoint = null;
  }

  public Rectangle getBounds() {
    return new Rectangle(this.position.x - 0.25f, this.position.y - 0.25f, 0.5f, 0.5f);
  }
  
  public Vector2f getPosition() {
    return this.position;
  }
  
  public void setPosition(final Vector2f position) {
    this.position = position.copy();
  }
  
  public Vector2f getVelocity() {
    return this.velocity;
  }
  
  public void calculateVelocity() {
    if (this.checkpoint != null) {
      this.velocity = this.checkpoint.copy();
    } else {
      this.velocity = this.goal.copy();
    }
    
    this.velocity.sub(this.position);
    
    if (this.velocity.length() > this.attributes.speed) {
      this.velocity.normalise();
      this.velocity.scale(this.attributes.speed);
    }
    
    if (Math.abs(this.velocity.x) >= Math.abs(this.velocity.y)) {
      // moving east or west
      if (this.velocity.x >= 0) {
        this.sprite.setFrameSequence(this.eastSequence);
      } else {
        this.sprite.setFrameSequence(this.westSequence);
      }
    } else {
      // moving north or south
      if (this.velocity.y <= 0) {
        this.sprite.setFrameSequence(this.northSequence);
      } else {
        this.sprite.setFrameSequence(this.southSequence);
      }
    }
  }
  
  public void setCheckpoint(Vector2f checkpoint) {
    this.checkpoint = checkpoint;
  }

  public float getValue() {
    return this.attributes.value;
  }

  public void draw(Graphics g, Rectangle tile) {
    Rectangle r = new Rectangle(tile.getCenterX() - tile.getWidth() / 4.0f, tile.getCenterY() - tile.getHeight() / 4.0f, tile.getWidth() / 2.0f, tile.getHeight() / 2.0f);
    this.sprite.draw(g, r);
  }
  
  public void update(float dt) {
    this.sprite.update(dt);
  }
  
  public void update(CreepManager cm, float dt) {
    this.update(dt);
    
    if (this.attributes.hp <= 0.0f) {
      cm.onDeath(this);
      return;
    }
    
    this.calculateVelocity();
  
    this.position.add(this.velocity.copy().scale(dt));
    
    if (this.checkpoint != null) {
      if (this.position.distanceSquared(this.checkpoint) < 0.5f * 0.5f) {
        cm.checkpointReached(this);
        return;
      }
    }
    
    if (this.position.distanceSquared(this.goal) < 0.5f * 0.5f) {
      cm.goalReached(this);
      return;
    }
  }

  public void takeDamage(Tower.Type source, float damage) {
    switch (this.attributes.type) {
    case AUDITOR:
      if (source == Tower.Type.ADVERT) {
        return;
      } else if (source == Tower.Type.CLERK) {
        this.attributes.hp -= damage;
      } else if (source == Tower.Type.SECURITY) {
        this.attributes.hp = 0;
      }
      
    case CUSTOMER:
      if (source == Tower.Type.ADVERT) {
        this.attributes.value += damage;
        this.attributes.hp -= damage;
        if (this.attributes.hp <= 0) {
          this.attributes.hp = Float.MIN_NORMAL;
        }
      } else if (source == Tower.Type.CLERK) {
        this.attributes.hp -= damage;
      } else if (source == Tower.Type.SECURITY) {
        this.attributes.value -= damage;
      }
      break;
      
    case HOBO:
      if (source == Tower.Type.ADVERT) {
        return;
      } else {
        this.attributes.hp -= damage;
      }
      break;
    }
  }
}
