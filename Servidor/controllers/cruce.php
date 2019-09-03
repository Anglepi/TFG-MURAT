<?php

class Cruce{
	public static function obtenerCruces(){
		$jwt = Funciones::checkJWT();
		$sql = "select * from cruces";
		$cruces = $GLOBALS["db"]->query($sql);
		$array_cruces = array();
		while($cruce = $cruces->fetch_array()){
			$array_cruces[] = $cruce;
		}
		$lista = '{"lista":'.json_encode($array_cruces).'}';
		echo $lista;
	}
/*
	public static function crearCruce(){
		//$jwt = Funciones::checkJWT();
		if(isset($_POST["nombreCruce"]) && $_POST["nombreCruce"] != ""){
			$nombreCruce = $GLOBALS['db']->escapeString($_POST["nombreCruce"]);
			$sql = "insert into cruces (`Nombre`) values ('".$nombreCruce."')";
			$sqlBusqueda = "select * from cruces where nombre = '".$nombreCruce."'";
			if(!$GLOBALS["db"]->resultRow($sqlBusqueda)){
				$GLOBALS["db"]->query($sql);
				echo '{"resultado":"OK", "mensaje":"Cruce introducido correctamente"}';
			} else {
				throw new Exception("Cruce ya existente");
			}
		} else {
			throw new Exception("Parametros no enviados");
		}
	}

	public static function actualizarCruce(){
		//$jwt = Funciones::checkJWT();
		if(isset($_POST["nombreCruce"]) && $_POST["nombreCruce"] != "" && isset($_POST["idCruce"]) && $_POST["idCruce"] != ""){
			$nombreCruce = $GLOBALS['db']->escapeString($_POST["nombreCruce"]);
			$idCruce = $GLOBALS['db']->escapeString($_POST["idCruce"]);
			$sql = "update cruces set Nombre = '".$nombreCruce."' where Id = '".$idCruce."'";
			$sqlBusqueda = "select * from cruces where Id = '".$idCruce."'";
			if($GLOBALS["db"]->resultRow($sqlBusqueda)){
				$GLOBALS["db"]->query($sql);
				echo '{"resultado":"OK", "mensaje":"Cruce actualizado correctamente"}';
			} else {
				echo '{"resultado":"ERROR", "mensaje":"Cruce no existente"}';
			}
		} else {
			throw new Exception("Parametros no enviados");
		}
	}

	public static function borrarCruce(){
		//$jwt = Funciones::checkJWT();
		if(isset($_POST["idCruce"]) && $_POST["idCruce"] != ""){
			$idCruce = $GLOBALS['db']->escapeString($_POST["idCruce"]);
			$sql = "delete from cruces where Id = '".$idCruce."'";
			
			if($GLOBALS["db"]->query($sql)){
				echo '{"resultado":"OK", "mensaje":"Cruce eliminado correctamente"}';
			} else {
				echo '{"resultado":"ERROR", "mensaje":"Cruce no existente"}';
			}
		} else {
			throw new Exception("Parametros no enviados");
		}
	}
*/
}
?>