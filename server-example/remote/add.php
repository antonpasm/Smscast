<?php
error_reporting(0);
header('Content-Type: application/json');

include "db_cfg.php";

$result = array("ok" => false);

// Create connection
$conn = new mysqli($dbserver, $dbuser, $dbpass, $dbname);
// Check connection
if ($conn->connect_error) {
//	$result["description"] = $conn->connect_error;
	die(json_encode($result));
}

if (isset($_GET['msg'])) {
	$message = $_GET['msg'] ;
	$message = $conn->real_escape_string($message);

	$sql = "INSERT INTO table1 (msg) VALUES ('$message')";

	if ($conn->query($sql) === TRUE) {
			$result["ok"] = true;
	} else {
//			$result["description"] = $conn->error;
	} 
}
$conn->close();

print(json_encode($result));

?>