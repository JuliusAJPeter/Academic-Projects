using System.Collections;
using System.Collections.Generic;
using UnityEngine;
using UnityEngine.SceneManagement;
using UnityEngine.UI;

public class Resetter : MonoBehaviour {

	public Rigidbody2D projectile;
	public float resetSpeed = 0.025f;

	private float resetSpeedSqr;
	private SpringJoint2D spring;

	public GameObject gameOver;
	public GameObject targetDamageObj;
	public GameObject collectCoinObj;

	private TargetDamage targetDamage;
	private CoinCollection coinCollection;
	public Text scoreTxt;
	public int scoreNo;

	void Reset(){
		SceneManager.LoadScene(0);
	}

	// Use this for initialization
	void Start () {
		resetSpeedSqr = resetSpeed * resetSpeed;
		spring = projectile.GetComponent<SpringJoint2D> ();
		targetDamage = targetDamageObj.GetComponent<TargetDamage>();
		coinCollection = collectCoinObj.GetComponent<CoinCollection> ();
		scoreNo = targetDamage.score + coinCollection.collect;
	}
	
	// Update is called once per frame
	void Update () {
		if (Input.GetKeyDown (KeyCode.R))
			Reset ();

		if (spring == null && projectile.velocity.sqrMagnitude < resetSpeedSqr) {
			scoreNo = targetDamage.score + coinCollection.collect;
			scoreTxt.text = "Score: " + scoreNo.ToString ();
			gameOver.SetActive (true);
		}
	}

	void OnTriggerExit2D(Collider2D other){
		if (other.GetComponent<Rigidbody2D> () == projectile) {
			gameOver.SetActive(true);
		}
	}
}
