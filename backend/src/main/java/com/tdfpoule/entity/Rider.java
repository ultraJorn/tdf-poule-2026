package com.tdfpoule.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "rider")
public class Rider {

    @Id
    @Column(length = 60)
    private String id;

    @Column(name = "poule_id", nullable = false, length = 8)
    private String pouleId;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 120)
    private String team;

    @Column(nullable = false)
    private int price;

    @Column(nullable = false, length = 40)
    private String tag;

    @Column(nullable = false)
    private boolean active = true;

    @Column(length = 2)
    private String nat;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getPouleId() { return pouleId; }
    public void setPouleId(String pouleId) { this.pouleId = pouleId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getTeam() { return team; }
    public void setTeam(String team) { this.team = team; }

    public int getPrice() { return price; }
    public void setPrice(int price) { this.price = price; }

    public String getTag() { return tag; }
    public void setTag(String tag) { this.tag = tag; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public String getNat() { return nat; }
    public void setNat(String nat) { this.nat = nat; }
}
