<?php
class configuracionCruce{
	public static function obtenerConfiguraciones(){
		//$jwt = Funciones::checkJWT();
		if(isset($_POST["idCruce"]) && $_POST["idCruce"] != ""){
			$idCruce = $GLOBALS['db']->escapeString($_POST["idCruce"]);
			$sql = "select c.Nombre, s.Nombre as NombreSemaforo, cs.EstadoSemaforo, c.Tiempo from configuracion c left join configuracion_semaforos cs on cs.IdConfiguracion = c.Id left join semaforos s on cs.IdSemaforo = s.Id where c.IdCruce = '".$idCruce."' order by c.Nombre";
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
/*
	public static function crearConfiguracion(){
		//$jwt = Funciones::checkJWT();
		if(isset($_POST["nombreConfiguracion"]) && $_POST["nombreConfiguracion"] != "" && isset($_POST["idCruce"]) && $_POST["idCruce"] != ""){
			$nombreConfiguracion = $GLOBALS['db']->escapeString($_POST["nombreConfiguracion"]);
			$idCruce  =$GLOBALS['db']->escapeString($_POST["idCruce"]);
			$sql = "insert into configuracion (`Nombre`, `IdCruce`) values ('".$nombreConfiguracion."', '".$idCruce."')";
			$sqlBusqueda = "select * from configuracion where nombre = '".$nombreConfiguracion."'";
			if(!$GLOBALS["db"]->resultRow($sqlBusqueda)){
				$GLOBALS["db"]->query($sql);
				echo '{"resultado":"OK", "mensaje":"Configuracion introducida correctamente"}';
			} else {
				throw new Exception("Configuracion ya existente");
			}
		} else {
			throw new Exception("Parametros no enviados");
		}
	}

	public static function actualizarConfiguracion(){
		//$jwt = Funciones::checkJWT();
		if(isset($_POST["nombreConfiguracion"]) && $_POST["nombreConfiguracion"] != "" && isset($_POST["idCruce"]) && $_POST["idCruce"] != "" && isset($_POST["idConfiguracion"]) && $_POST["idConfiguracion"] != ""){
			$nombreConfiguracion = $GLOBALS['db']->escapeString($_POST["nombreConfiguracion"]);
			$idCruce = $GLOBALS['db']->escapeString($_POST["idCruce"]);
			$idConfiguracion = $GLOBALS['db']->escapeString($_POST["idConfiguracion"]);
			$sql = "update configuracion set Nombre = '".$nombreConfiguracion."', IdCruce = '".$idCruce."' where Id = '".$idConfiguracion."'";
			$sqlCompruebaCruce = "select * from cruces where Id = '".$idCruce."'";
			
			if($GLOBALS["db"]->resultRow($sqlCompruebaCruce)){
				if($GLOBALS["db"]->query($sql)){
					echo '{"resultado":"OK", "mensaje":"Configuracion actualizada correctamente"}';
				} else {
					echo '{"resultado":"ERROR", "mensaje":"Configuracion no existente"}';
				}
			} else {
				throw new Exception("Cruce no existente");
			}
		} else {
			throw new Exception("Parametros no enviados");
		}
	}

	public static function borrarConfiguracion(){
		//$jwt = Funciones::checkJWT();
		if(isset($_POST["idConfiguracion"]) && $_POST["idConfiguracion"] != ""){
			$idConfiguracion = $GLOBALS['db']->escapeString($_POST["idConfiguracion"]);
			$sql = "delete from configuracion where Id = '".$idConfiguracion."'";
			
			$GLOBALS["db"]->query($sql);
				
			$sql = "delete from configuracion_semaforos where IdConfiguracion = '".$idConfiguracion."'";
			$GLOBALS["db"]->query($sql);

			echo '{"resultado":"OK", "mensaje":"Configuracion eliminada correctamente"}';
			
		} else {
			throw new Exception("Parametros no enviados");
		}
	}

	public static function establecerConfiguracion(){
		//$jwt = Funciones::checkJWT();
		if(isset($_POST["idConfiguracion"]) && $_POST["idConfiguracion"] != "" && isset($_POST["semaforos"]) && $_POST["semaforos"] != ""){
			//Comprobar que la configuracion está establecida, y si lo está: eliminarla
			$idConfiguracion = $GLOBALS['db']->escapeString($_POST["idConfiguracion"]);
			$sql = "select * from configuracion_semaforos where IdConfiguracion = '".$idConfiguracion."'";
			$configuracionEstablecida = $GLOBALS["db"]->query($sql)->fetch_array();

			$sql = "select * from configuracion where Id = '".$idConfiguracion."'";
			$configuracionExistente = $GLOBALS["db"]->query($sql)->fetch_array();
			if(!$configuracionExistente){
				throw new Exception("La configuracion indicada no existe");
			}
			$idCruce = $configuracionExistente["IdCruce"];



			$arraySemaforos = json_decode($_POST["semaforos"]);
			if($arraySemaforos == NULL){
				throw new Exception("JSON mal formado");
			}


			//obtener todos los semaforos de un cruce
			$sql = "select IdSemaforo from cruces_semaforos where IdCruce = '$idCruce' order by IdSemaforo ASC";
			$semaforosCruce = $GLOBALS["db"]->resultMatrix($sql);

			if(count($semaforosCruce) != count($arraySemaforos)){
				throw new Exception("No se han indicado todos los semaforos del cruce a configurar");
			}

			$numSemaforos = count($semaforosCruce);
			for($i=0 ; $i<$numSemaforos ; $i++){
				if($semaforosCruce[$i]["IdSemaforo"] != $arraySemaforos[$i]->id){
					echo $semaforosCruce[$i]["IdSemaforo"];
					echo $arraySemaforos[$i]->id;
					throw new Exception("Los semáforos del cruce y los establecidos en la configuración no coinciden");
				}
			}
			
			if($configuracionEstablecida){
				$sql = "delete from configuracion_semaforos where IdConfiguracion = '".$idConfiguracion."'";
				$GLOBALS["db"]->query($sql);
			}

			$sql = "insert into configuracion_semaforos (`IdConfiguracion`, `IdSemaforo`, `EstadoSemaforo`) values ";
			foreach ($arraySemaforos as $semaforo) {
				$sql .= "('$idConfiguracion', '$semaforo->id', '$semaforo->estado'),";
			}
			$sql = substr($sql, 0, -1);

			$GLOBALS["db"]->query($sql);

			echo '{"resultado":"OK","mensaje":"Configuración establecida con éxito"}';
		} else {
			throw new Exception("Parametros no enviados correctamente");
		}
	}
*/
}
?>