<?php

function main(){
	
	$params = @json_decode(file_get_contents("cfg.json"), true);
	
	$data = @json_decode(file_get_contents($params['url']), true);
	
	if (!is_array($data))
		return;
	
	if (!@$data['ok'] || !is_array(@$data['messages']))
		return;
	
	foreach($data['messages'] as $row) {
		$encoded = url_safe_base64_decode($row['msg']);
		$msg = decode($encoded, $params['password']);
		if ($msg === false) {
			print("Invalid password?\r\n");
			$msg = $encoded;
		}

		$msg = @iconv("UTF-8", "cp866", $msg);
		if ($msg === false) {
			print("skip message " . $row['msg'] . "\r\n\r\n");
			continue;
		}
		
		$lines = preg_split("/\r\n/", $msg);
		if (count($lines) > 0 && is_numeric($lines[0])) {
			$dt = new DateTime("@" . intdiv($lines[0], 1000));
			$dt->setTimezone(new DateTimeZone('Europe/Moscow'));
			$lines[0] = $dt->format("Y-m-d H:i:s");
		}
		print(implode("\r\n", $lines) . "\r\n\r\n");
	}
};

function url_safe_base64_decode($data) {
	$data = base64_decode(str_replace(array('-','_'), array('+','/'), $data));
	return $data;	
}
 
function decode($encoded, $password) {
	if ($password == "")
		return $encoded;
	
	$cipher = "AES-128-CBC";

	$hashkey = substr(sha1($password, true), 0, 16);
	$iv = substr(sha1($hashkey, true), 0, 16);
	
	if ($plaintext = openssl_decrypt($encoded, $cipher, $hashkey, OPENSSL_RAW_DATA, $iv)) {
		return $plaintext;
	} else {
		return false;
	}
}


main();
