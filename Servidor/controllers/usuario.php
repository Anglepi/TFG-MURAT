<?php
use \Firebase\JWT\JWT;

class Usuario{
	public static function autenticar(){
		if(isset($_POST["usuario"]) && isset($_POST["clave"])){
			$usuario = $_POST["usuario"];
			$clave = $_POST["clave"];

			if($usuario == "") throw new Exception("Campo usuario vacío");
			if($clave == "") throw new Exception("Campo clave vacío");

			$sql = "select count(*) from usuarios where Usuario = '".$GLOBALS["db"]->escapeString($usuario)."'";
			$existe = $GLOBALS["db"]->resultRow($sql);
			
			if($existe){
				$sql = "select * from usuarios where Usuario = '".$GLOBALS["db"]->escapeString($usuario)."'";
				$user = $GLOBALS["db"]->resultRow($sql);

				if(password_verify($clave, $user["Clave"])){
					$token = array(
					    "exp" => time() + 600,
					    "data" => array("userID" => $user["Id"])
					);

					$jwt = JWT::encode($token, "dsnqkwex");
					echo '{"token":"'.$jwt.'"}';
				}
				else{
					echo "Identificado mal";
				}
			}
			
			

		} else {
			throw new Exception("Parametros no enviados");
		}
	}

	public static function generarUsuario(){
		if(isset($_POST["usuario"]) && isset($_POST["clave"])){
			$usuario = $_POST["usuario"];
			$clave = $_POST["clave"];
			$nombre = (isset($_POST["nombre"]) ? $_POST["nombre"] : "");

			if($usuario == "") throw new Exception("Campo usuario vacío");
			if($clave == "") throw new Exception("Campo clave vacío");

			$clave = password_hash($clave, PASSWORD_DEFAULT);

			$sql = "select count(*) from usuarios where Usuario = '".$GLOBALS["db"]->escapeString($usuario)."'";
			$existe = $GLOBALS["db"]->resultRow($sql);
			if($existe) throw new Exception("El usuario ya existe, escoge otro");

			echo '{"usuario":"'.$usuario.'","clave":"'.$clave.'","nombre":"'.$nombre.'"}'; //{"usuario":"angel","clave":"mdi23hh(H/DAS"}
		} else {
			throw new Exception("Parametros no enviados");
		}
	}
}
?>