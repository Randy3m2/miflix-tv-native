'use strict';
const fs=require('fs'),vm=require('vm'),assert=require('assert');
const code=fs.readFileSync(process.argv[2],'utf8');
const snippet=code.slice(code.indexOf('function validCastAddress('),code.indexOf('function castRender('));
const context=vm.createContext({URL,t:x=>x});vm.runInContext(snippet,context);
for(const value of ['192.168.1.20:8765','http://10.0.0.8:8765','http://172.16.0.2:8765','172.31.255.1:8765'])assert(context.validCastAddress(value).startsWith('http://'));
for(const value of ['https://192.168.1.20:8765','8.8.8.8:8765','127.0.0.1:8765','172.32.0.1:8765','192.168.1.20:80','192.168.1.20:8765/path','user:pass@192.168.1.20:8765','192.168.1.20:8765/?token=secret'])assert.throws(()=>context.validCastAddress(value));
console.log('PASS: local endpoint validation, public/loopback rejection, protocol/port/path/credential restrictions');
