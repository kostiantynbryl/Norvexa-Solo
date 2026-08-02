package com.norvexa.flow.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.AutoGraph
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.norvexa.flow.domain.parseMinor

@Composable
fun OnboardingScreen(onComplete:(String,Int,Long,String,Long)->Unit){
    var page by remember{mutableIntStateOf(0)};var currency by remember{mutableStateOf("USD")};var tax by remember{mutableStateOf("10")};var safe by remember{mutableStateOf("500")};var wallet by remember{mutableStateOf("Основной кошелёк")};var balance by remember{mutableStateOf("0")}
    Surface(Modifier.fillMaxSize()){
        Column(Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.SpaceBetween){
            Column(verticalArrangement=Arrangement.spacedBy(18.dp)){
                Text("Norvexa Flow",style=MaterialTheme.typography.headlineLarge,fontWeight=FontWeight.Bold)
                when(page){
                    0->Intro(Icons.Rounded.AutoGraph,"Планируйте деньги вперёд","Приложение показывает доступные средства, будущие платежи и возможный кассовый разрыв.")
                    1->Intro(Icons.Rounded.Lock,"Ваши данные остаются на устройстве","Банковские аккаунты и регистрация не требуются. Основные функции работают без интернета.")
                    else->{
                        Intro(Icons.Rounded.AccountBalanceWallet,"Начальная настройка","Укажите базовую валюту, ориентировочный резерв и первый кошелёк.")
                        OutlinedTextField(currency,{currency=it.uppercase().take(3)},label={Text("Базовая валюта")},singleLine=true,modifier=Modifier.fillMaxWidth())
                        OutlinedTextField(tax,{tax=it.filter(Char::isDigit).take(2)},label={Text("Плановый налоговый резерв, %")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),singleLine=true,modifier=Modifier.fillMaxWidth())
                        OutlinedTextField(safe,{safe=it},label={Text("Безопасный остаток")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Decimal),singleLine=true,modifier=Modifier.fillMaxWidth())
                        OutlinedTextField(wallet,{wallet=it},label={Text("Название кошелька")},singleLine=true,modifier=Modifier.fillMaxWidth())
                        OutlinedTextField(balance,{balance=it},label={Text("Текущий баланс")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Decimal),singleLine=true,modifier=Modifier.fillMaxWidth())
                        Text("Norvexa Flow — инструмент личного планирования, а не бухгалтерская, налоговая или юридическая консультация.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(top=24.dp),horizontalArrangement=Arrangement.SpaceBetween){
                if(page>0)TextButton(onClick={page--}){Text("Назад")}else Spacer(Modifier.width(1.dp))
                Button(enabled=page<2||(currency.length==3&&wallet.isNotBlank()&&tax.toIntOrNull() in 0..95&&parseMinor(safe)!=null&&parseMinor(balance)!=null),onClick={if(page<2)page++ else onComplete(currency,tax.toIntOrNull()?:0,parseMinor(safe)?:0,wallet,parseMinor(balance)?:0)}){Text(if(page<2)"Далее" else "Начать")}
            }
        }
    }
}

@Composable private fun Intro(icon:androidx.compose.ui.graphics.vector.ImageVector,title:String,text:String){Column(verticalArrangement=Arrangement.spacedBy(12.dp)){Icon(icon,null,tint=MaterialTheme.colorScheme.primary,modifier=Modifier.size(56.dp));Text(title,style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.SemiBold);Text(text,style=MaterialTheme.typography.bodyLarge,color=MaterialTheme.colorScheme.onSurfaceVariant)}}
