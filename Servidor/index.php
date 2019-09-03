<?php
require __DIR__.'/vendor/autoload.php';
	require_once 'model/BaseDatos.php';
	require_once 'utilidades/funciones.php';
	$db = new BaseDatos();
	require_once 'controllers/usuario.php';
	require_once 'controllers/cruce.php';
	require_once 'controllers/semaforo.php';
	require_once 'controllers/configuracionCruce.php';


	if(isset($_SERVER["PATH_INFO"]) && $_SERVER["PATH_INFO"] != ""){
		
		$path = substr($_SERVER["PATH_INFO"], 1);
		$path = explode("/", $path);

		if(count($path) == 2){

			$clases = array('cruce', 'semaforo', 'usuario', 'configuracionCruce');

			if(in_array($path[0], $clases) && method_exists($path[0], $path[1])){
				
				call_user_func($path[0]."::".$path[1]);
				$db->desconectar();
			}
				
			else {
				throw new Exception("Recurso solicitado no válido");
			}

		} else {
			throw new Exception("Formato de path no válido");
		}
			
	} else {
		throw new Exception("Debes especificar un path");
	}
?>

