<?php
use \Firebase\JWT\JWT;
class Funciones{
	public static function getJWT(){
		$authHeader = getallheaders()["Authorization"];
		if(substr($authHeader,0,7)!='Bearer ') return false;
		else return trim(substr($authHeader,7));
	}

	public static function checkJWT(){
		$token = Funciones::getJWT();

		return JWT::decode($token, "dsnqkwex", array('HS256'));
	}
}
?>