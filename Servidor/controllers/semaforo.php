<?php
class Semaforo{
	public static function obtenerSemaforos(){
		$jwt = Funciones::checkJWT();
		if(isset($_POST["idCruce"]) && $_POST["idCruce"] != ""){
			$idCruce = $GLOBALS['db']->escapeString($_POST["idCruce"]);
			$sql = "select * from semaforos s left join cruces_semaforos cs on cs.IdSemaforo = s.Id where cs.IdCruce = '".$idCruce."'";
			$cruces = $GLOBALS["db"]->query($sql);
			$array_cruces = array();
			while($cruce = $cruces->fetch_array()){
				$array_cruces[] = $cruce;
			}
			$lista = '{"lista":'.json_encode($array_cruces).'}';
			echo $lista;
		} else {
			throw new Exception("Parametros no enviados");
		}
	}

	public static function obtenerOrigenesSemaforo(){
		//$jwt = Funciones::checkJWT();
		if(isset($_POST["idSemaforo"]) && $_POST["idSemaforo"] != ""){
			$idSemaforo = $GLOBALS['db']->escapeString($_POST["idSemaforo"]);
			$sql = "SELECT Nombre FROM conexiones_semaforos cs INNER JOIN semaforos s ON cs.Origen = s.Id WHERE cs.Destino = '".$idSemaforo."'";
			$semaforos = $GLOBALS["db"]->query($sql);
			$array_semaforos = array();
			while($semaforo = $semaforos->fetch_array()){
				$array_semaforos[] = $semaforo;
			}
			$lista = '{"lista":'.json_encode($array_semaforos).'}';
			echo $lista;
		} else {
			throw new Exception("Parametros no enviados");
		}
	}

	public static function obtenerDestinosSemaforo(){
		//$jwt = Funciones::checkJWT();
		if(isset($_POST["idSemaforo"]) && $_POST["idSemaforo"] != ""){
			$idSemaforo = $GLOBALS['db']->escapeString($_POST["idSemaforo"]);
			$sql = "SELECT Nombre, ProporcionVehiculos, Destino FROM conexiones_semaforos cs LEFT JOIN semaforos s ON cs.Destino = s.Id WHERE cs.Origen = '".$idSemaforo."'";
			$semaforos = $GLOBALS["db"]->query($sql);
			$array_semaforos = array();
			while($semaforo = $semaforos->fetch_array()){
				$array_semaforos[] = $semaforo;
			}
			$lista = '{"lista":'.json_encode($array_semaforos).'}';
			echo $lista;
		} else {
			throw new Exception("Parametros no enviados");
		}
	}

	public static function obtenerTodosSemaforos(){
		//$jwt = Funciones::checkJWT();
		
		$sql = "SELECT s.*, c.Nombre as NombreCruce FROM semaforos s LEFT JOIN cruces_semaforos cs ON s.Id = cs.IdSemaforo LEFT JOIN cruces c ON c.Id = cs.IdCruce";
		$semaforos = $GLOBALS["db"]->query($sql);
		$array_semaforos = array();
		while($semaforo = $semaforos->fetch_array()){
			$array_semaforos[] = $semaforo;
		}
		$lista = '{"lista":'.json_encode($array_semaforos).'}';
		echo $lista;
		
	}
/*
	public static function obtenerCruceSemaforo(){
		//$jwt = Funciones::checkJWT();
		if(isset($_POST["idSemaforo"]) && $_POST["idSemaforo"] != ""){
			$idSemaforo = $GLOBALS['db']->escapeString($_POST["idSemaforo"]);
			$sql = "select * from cruces_semaforos where IdSemaforo = '".$idSemaforo."'";
			$cruces = $GLOBALS["db"]->query($sql);
			$array_cruces = array();
			while($cruce = $cruces->fetch_array()){
				$array_cruces[] = $cruce;
			}
			$lista = '{"resultado":"OK", "idCruce":"'.$array_cruces["IdCruce"].'"}';
			echo $lista;
		} else {
			throw new Exception("Parametros no enviados");
		}
		
	}

	public static function crearSemaforo(){
		//$jwt = Funciones::checkJWT();
		if(isset($_POST["nombreSemaforo"]) && $_POST["nombreSemaforo"] != ""){
			$nombreSemaforo = $GLOBALS['db']->escapeString($_POST["nombreSemaforo"]);
			$idPasoPeatones = ((isset($_POST["idPasoPeatones"]) && $_POST["idPasoPeatones"] != "") ? $GLOBALS['db']->escapeString($_POST["idPasoPeatones"]) : 0);
			$sql = "insert into semaforos (`Nombre`, `IdPasoPeatones`) values ('".$nombreSemaforo."', '".$idPasoPeatones."')";
			$sqlBusqueda = "select * from semaforos where nombre = '".$nombreSemaforo."'";
			$sqlBusquedaPasoPeatones = "select * from pasos_peatones where id = '".$idPasoPeatones."'";
			if(!$GLOBALS["db"]->resultRow($sqlBusqueda)){
				if($idPasoPeatones == 0 || $GLOBALS["db"]->resultRow($sqlBusquedaPasoPeatones)){
					$GLOBALS["db"]->query($sql);
					echo '{"resultado":"OK", "mensaje":"Semaforo introducido correctamente"}';
				} else {
					throw new Exception("Paso de peatones seleccionado no existente");
				}
			} else {
				throw new Exception("Semaforo ya existente");
			}
		} else {
			throw new Exception("Parametros no enviados");
		}
	}

	public static function actualizarSemaforo(){
		//$jwt = Funciones::checkJWT();
		if(isset($_POST["nombreSemaforo"]) && $_POST["nombreSemaforo"] != "" && isset($_POST["idPasoPeatones"]) && $_POST["idPasoPeatones"] != "" && isset($_POST["idSemaforo"]) && $_POST["idSemaforo"] != ""){
			$nombreSemaforo = $GLOBALS['db']->escapeString($_POST["nombreSemaforo"]);
			$idPasoPeatones = $GLOBALS['db']->escapeString($_POST["idPasoPeatones"]);
			$idSemaforo = $GLOBALS['db']->escapeString($_POST["idSemaforo"]);
			$sql = "update semaforos set Nombre = '".$nombreSemaforo."', IdPasoPeatones = '".$idPasoPeatones."' where Id = '".$idSemaforo."'";
			
			if($GLOBALS["db"]->query($sql)){
				echo '{"resultado":"OK", "mensaje":"Semaforo actualizado correctamente"}';
			} else {
				echo '{"resultado":"ERROR", "mensaje":"Semaforo no existente"}';
			}
		} else {
			throw new Exception("Parametros no enviados");
		}
	}

	public static function borrarSemaforo(){
		//$jwt = Funciones::checkJWT();
		if(isset($_POST["idSemaforo"]) && $_POST["idSemaforo"] != ""){
			$idSemaforo = $GLOBALS['db']->escapeString($_POST["idSemaforo"]);
			$sql = "delete from semaforos where Id = '".$idSemaforo."'";
			
			if($GLOBALS["db"]->query($sql)){
				echo '{"resultado":"OK", "mensaje":"Semaforo eliminado correctamente"}';
			} else {
				echo '{"resultado":"ERROR", "mensaje":"Ha habido un problema al eliminar el semáforo"}';
			}
		} else {
			throw new Exception("Parametros no enviados");
		}
	}
	*/
}
?>