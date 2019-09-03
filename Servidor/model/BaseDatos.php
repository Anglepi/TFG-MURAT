<?php
class BaseDatos{
	private $conectado;
	private $conexion;

	function __construct(){
		$host = "localhost";
		$baseDatos = "murat";
		$usuario = "root";
		$clave = "caligrafia";
		$this->conexion = mysqli_connect($host, $usuario, $clave, $baseDatos);
		$this->conexion->set_charset("utf8");
		$this->conectado = true;
		if(!$this->conexion){
			echo "Error: no se ha podido conectar con la base de datos";
			$this->conectado = false;
			exit;
		}
	}

	function desconectar(){
		if($this->conectado){
			$this->conectado = false;
			mysqli_close($this->conexion);
		}
		return $this->conectado;
	}

	function conectar(){
		if(!$this->conectado){
			$this->conexion = mysqli_connect($host, $usuario, $clave, $baseDatos);
			$this->conexion->set_charset("utf8");
			$this->conectado = true;
			if(!$this->conexion){
				echo "Error: no se ha podido conectar con la base de datos";
				$this->conectado = false;
			}
		}
		return $this->conectado;
	}

	function resultRow($sql){
		if($this->conectado){
			$result = $this->conexion->query($sql)->fetch_array();
			
			if($result) return $result;
			else return false;
		}
	}

	function resultMatrix($sql){
		if($this->conectado){
			$matrix = array();
			$result = $this->conexion->query($sql);
			while($row = $result->fetch_array()){
				$matrix[] = $row;
			}
			
			if(count($matrix)>0) return $matrix;
			else return false;
		}
	}

	function query($sql){
		if($this->conectado){
			$result = $this->conexion->query($sql);
			
			if($result) return $result;
			else return false;
		}
	}

	function escapeString($string){
		return $this->conexion->real_escape_string($string);
	}

}
?>