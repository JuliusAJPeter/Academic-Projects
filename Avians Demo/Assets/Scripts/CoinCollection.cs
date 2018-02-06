using System.Collections;
using System.Collections.Generic;
using UnityEngine;

public class CoinCollection : MonoBehaviour {

	public int hitPoints = 5;
	public Sprite collectSprite;
	public float collectImpactSpeed = 0.3f;
	public int collect = 0;

	private int currHitPoints;
	private float collectImpactSpeedSqr;
	private SpriteRenderer spriteRenderer;

	// Use this for initialization
	void Start () {
		spriteRenderer = GetComponent<SpriteRenderer> ();
		currHitPoints = hitPoints;
		collectImpactSpeedSqr = collectImpactSpeed * collectImpactSpeed;
	}

	// Update is called once per frame
	void Update () {

	}

	void OnCollisionEnter2D(Collision2D collision){
		if (collision.collider.tag != "Damager")
			return;

		currHitPoints--;

		if (collision.relativeVelocity.sqrMagnitude < collectImpactSpeedSqr) {
			spriteRenderer.sprite = collectSprite;
			return;
		}

		if (currHitPoints <= 0)
			Vanish ();
	}

	void Vanish(){
		spriteRenderer.enabled = false;
		GetComponent<Collider2D>().enabled = false;
		GetComponent<Rigidbody2D>().isKinematic = true;
		collect = 50;
	}
}
