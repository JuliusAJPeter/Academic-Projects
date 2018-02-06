using System.Collections;
using System.Collections.Generic;
using UnityEngine;

public class TargetDamage : MonoBehaviour {

	public int hitPoints = 5;
	public Sprite damagedSprite;
	public float damageImpactSpeed = 0.3f;
	public int score = 0;

	private int currHitPoints;
	private float damageImpactSpeedSqr;
	private SpriteRenderer spriteRenderer;

	// Use this for initialization
	void Start () {
		spriteRenderer = GetComponent<SpriteRenderer> ();
		currHitPoints = hitPoints;
		damageImpactSpeedSqr = damageImpactSpeed * damageImpactSpeed;
	}
	
	// Update is called once per frame
	void Update () {
		
	}

	void OnCollisionEnter2D(Collision2D collision){
		if (collision.collider.tag != "Damager")
			return;
	
		currHitPoints--;

		if (collision.relativeVelocity.sqrMagnitude < damageImpactSpeedSqr) {
			spriteRenderer.sprite = damagedSprite;
			return;
		}

		if (currHitPoints <= 0)
			Kill ();
	}

	void Kill(){
		spriteRenderer.enabled = false;
		GetComponent<Collider2D>().enabled = false;
		GetComponent<Rigidbody2D>().isKinematic = true;
		ParticleSystem particleSystem = GetComponent<ParticleSystem>();
		particleSystem.Play ();
		score = 10;
	}
}
