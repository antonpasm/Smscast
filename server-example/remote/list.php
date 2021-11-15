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

$sql = "SELECT dt, msg FROM table1 order by dt ASC";

$data = array();
if ($res = $conn->query($sql)) {
	while ($row = $res->fetch_assoc()) {
		$data[] = $row;
	}
	$result["ok"] = true;
	$result["messages"] = $data;
} else {
//	$result["description"] = $conn->error;
}
$conn->close();

print(json_encode($result));

?>