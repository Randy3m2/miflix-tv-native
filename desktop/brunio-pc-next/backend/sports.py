"""Pure PC sports adapters extracted from the existing desktop implementation."""
import datetime as dt

def scoreboard(transport,league,month=None):
    if league=='F1' and month:
        year=month[:4];data=transport('https://api.jolpi.ca/ergast/f1/'+year+'.json?limit=100')
        races=data.get('MRData',{}).get('RaceTable',{}).get('Races',[])
        return [{'id':str(x.get('round','')),'league':'F1','title':x.get('raceName','F1'),'starts':x.get('date','')+'T'+x.get('time','00:00:00Z'),'teams':[],'scores':[],'logos':[],'status':''} for x in races if x.get('date','').startswith(month)]
    if league in ('NBA','F1'):
        sport='basketball/nba' if league=='NBA' else 'racing/f1'
        suffix=('?dates='+month.replace('-','')+'&limit=1000') if month and league=='NBA' else ''
        data=transport('https://site.api.espn.com/apis/site/v2/sports/'+sport+'/scoreboard'+suffix)
        result=[]
        for event in data.get('events',[]):
            comp=(event.get('competitions') or [{}])[0];teams=comp.get('competitors') or []
            result.append({'id':str(event.get('id','')),'league':league,'title':event.get('name',''),'starts':event.get('date',''),
                'teams':[str((x.get('team') or {}).get('id','')) for x in teams],
                'scores':[str(x.get('score','')) for x in teams],
                'logos':[(x.get('team') or {}).get('logo','') for x in teams],
                'status':(event.get('status') or comp.get('status') or {}).get('type',{}).get('shortDetail',''),
                'drivers':[{'name':(x.get('athlete') or {}).get('shortName') or (x.get('athlete') or {}).get('displayName',''),
                    'position':x.get('order','—'),'photo':((x.get('athlete') or {}).get('headshot') or {}).get('href',''),
                    'stats':{v.get('name',''):v.get('displayValue','—') for v in x.get('statistics',[])}} for x in teams if x.get('athlete')]})
        return result
    if league in ('MLB','LIDOM'):
        if month:
            first=dt.date.fromisoformat(month+'-01');last=(first.replace(day=28)+dt.timedelta(days=4)).replace(day=1)-dt.timedelta(days=1)
        else:first=last=dt.date.today()
        sport='1' if league=='MLB' else '17&leagueId=131'
        data=transport(f'https://statsapi.mlb.com/api/v1/schedule?sportId={sport}&startDate={first}&endDate={last}')
        result=[]
        for day in data.get('dates',[]):
            for g in day.get('games',[]):
                teams=g.get('teams',{});away=teams.get('away',{});home=teams.get('home',{});a=away.get('team',{});h=home.get('team',{})
                result.append({'id':str(g.get('gamePk','')),'league':league,'title':a.get('name','')+' vs '+h.get('name',''),
                    'starts':g.get('gameDate',day.get('date','')),'teams':[str(a.get('id','')),str(h.get('id',''))],
                    'scores':[str(away.get('score','')),str(home.get('score',''))],
                    'logos':[f"https://www.mlbstatic.com/team-logos/{x.get('id','')}.svg" for x in (a,h)] if league=='MLB' else [],
                    'status':(g.get('status') or {}).get('detailedState','')})
        return result
    return []

def follows(subs,event):
    league=event['league'];return league+':*' in subs or 'EVENT:'+league+':'+event['id'] in subs or any(league+':'+x in subs for x in event.get('teams',[]))
