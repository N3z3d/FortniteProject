import { Injectable } from '@angular/core';
import { FortnitePlayer, FortniteTeam, ChampionshipData, RegionStats } from '../models/fortnite-player.interface';

/**
 * Service pour parser et organiser les données CSV des joueurs Fortnite
 */
@Injectable({
  providedIn: 'root'
})
export class CsvDataService {

  /**
   * Données CSV complètes parsées à partir du fichier fortnite_data.csv
   * Format: Pronostiqueur,Joueur,Région attribué,Score PR,Classement,Basé 2024
   */
  private readonly csvData: string = `Pronostiqueur,Joueur,Région attribué,Score PR,Classement,Basé 2024
Marcel,pixie,EU,108022,1,26-30
Marcel,Muz,NAC,125360,2,16-20
Marcel,White,BR,82413,2,NEW
Marcel,5aald,ME,85869,5,1-5
Marcel,Nuti,BR,80444,5,6-10
Marcel,Sphinx,NAC,108889,7,11-15
Marcel,Scarpa,BR,78767,7,21-25
Marcel,Meah,ASIA,80166,8,21-25
Marcel,mannii14,NAW,72642,10,1-5
Marcel,Aspect,OCE,77655,11,1-5
Marcel,Gazer,OCE,74690,13,21-25
Marcel,らぜる,ASIA,75439,14,16-20
Marcel,Acorn,NAC,116664,15,1-5
Marcel,EDGE ライトセーバー,ASIA,71270,18,6-10
Marcel,SwizzY,EU,77387,20,6-10
Marcel,Hris,EU,77488,21,21-25
Marcel,Stryker,BR,72637,21,1-5
Marcel,Wqzzi,ME,61987,22,26-30
Marcel,Rainy,ASIA,62083,23,11-15
Marcel,Cadu,BR,71131,24,31-infini
Marcel,Reet,NAC,88644,26,NEW
Marcel,Mansour,ME,60160,26,11-15
Marcel,tjino 1,EU,73318,27,31-infini
Marcel,Chap,EU,72041,28,1-5
Marcel,Arrow,ME,59659,28,6-10
Marcel,Nebs,EU,82317,30,11-15
Marcel,Tisco,BR,66837,30,26-30
Marcel,Fisher,OCE,62366,30,11-15
Marcel,Antonio,NAW,57436,30,26-30
Marcel,QnDx,ME,56823,30,31-infini
Marcel,Tame,OCE,62561,31,NEW
Marcel,Spoctic,OCE,61721,32,26-30
Marcel,Spy,ME,56040,32,16-20
Marcel,skqttles,NAC,99863,33,NEW
Marcel,Massimo,OCE,61024,33,6-10
Marcel,Fahad,ME,55388,33,NEW
Marcel,Pinq,EU,71347,34,NEW
Marcel,TheFeloz Balboa,BR,65580,34,NEW
Marcel,かめてぃん.魔女,ASIA,62674,34,NEW
Marcel,Phazma,OCE,60704,35,31-infini
Marcel,xenonfv,NAW,56048,36,31-infini
Marcel,Alex,NAW,85716,37,16-20
Marcel,fno clukzǃ,NAW,54032,38,6-10
Marcel,flinty,NAW,54449,39,11-15
Marcel,HST stella,ASIA,57323,40,NEW
Marcel,Avivv,NAC,65914,43,6-10
Marcel,remiǃ,NAW,52035,43,21-25
Marcel,Mason,NAC,66645,44,31-infini
Marcel,mxrxk,ASIA,54845,50,NEW
Teddy,Peterbot,NAC,143509,1,31-infini
Teddy,ふーくん,ASIA,101818,1,1-5
Teddy,Oatley,OCE,97364,1,31-infini
Teddy,PXMP,NAW,144041,3,11-15
Teddy,Eomzo,NAC,138969,3,21-25
Teddy,Koyota,ASIA,105246,3,31-infini
Teddy,Wreckless,OCE,92210,3,16-20
Teddy,KING,BR,80913,3,31-infini
Teddy,Parz,NAW,128110,4,6-10
Teddy,Kchorro,BR,80620,4,11-15
Teddy,Higgs,NAC,131763,5,11-15
Teddy,Vic0,EU,100304,5,1-5
Teddy,Sxhool,NAW,86191,5,1-5
Teddy,Clix,NAC,125885,6,6-10
Teddy,Bacca,NAW,118955,6,26-30
Teddy,Minipiyo,ASIA,86984,6,26-30
Teddy,BySaLva,ME,84628,6,6-10
Teddy,Kalgamer,ME,78032,7,21-25
Teddy,Tinka,OCE,82446,8,21-25
Teddy,Kami,EU,113255,9,26-30
Teddy,Fredoxie,EU,103146,11,16-20
Teddy,Night,BR,77585,11,6-10
Teddy,THORIK,NAW,74528,11,16-20
Teddy,Pollo,NAC,109376,12,1-5
Teddy,Wox,EU,86924,17,11-15
Teddy,Khanada,NAC,99377,19,26-30
Teddy,Deymoǃ,OCE,69844,19,1-5
Teddy,Setty,EU,103017,22,6-10
Teddy,Sanjog,OCE,67310,22,6-10
Teddy,EpikWhale,NAW,76712,23,31-infini
Teddy,Ajerss,NAC,105326,25,16-20
Teddy,VortexM,OCE,64648,27,26-30
Teddy,FHD,ME,59659,28,26-30
Teddy,Phzin,BR,67197,29,21-25
Teddy,Th0masHD,EU,89936,32,31-infini
Teddy,XMipoli,ASIA,58784,35,6-10
Teddy,Thiagin,BR,64622,37,1-5
Teddy,Protoon,OCE,57468,41,11-15
Teddy,Snowy,ME,50875,42,11-15
Teddy,TruleX,EU,63262,44,21-25
Teddy,Hajuu,NAW,52805,44,21-25
Teddy,Tecne,BR,58958,50,16-20
Teddy,Escdark,ME,48774,50,1-5
Teddy,P5EK,ME,48094,51,31-infini
Teddy,KBR,BR,54113,63,26-30
Teddy,Zerokun,ASIA,51054,65,21-25
Teddy,Peterbotǃ 33,ASIA,45946,75,11-15
Teddy,7man,ME,29753,102,16-20
Teddy,Pepoclip,ASIA,31410,166,16-20
Thibaut,FKS,ME,88445,1,1-5
Thibaut,MariusCOW,EU,122733,2,16-20
Thibaut,Salko,NAW,93482,2,16-20
Thibaut,Rew,ME,88366,2,21-25
Thibaut,Hero,ME,87533,3,11-15
Thibaut,Rapid,NAC,120243,4,11-15
Thibaut,Flickzy,EU,99145,4,1-5
Thibaut,yuma,ASIA,92158,5,1-5
Thibaut,alex,OCE,85716,5,11-15
Thibaut,Malibuca,EU,120095,6,31-infini
Thibaut,Anon,OCE,82817,6,21-25
Thibaut,Veno,EU,95179,7,26-30
Thibaut,Cazi,OCE,81986,7,26-30
Thibaut,charyy,EU,125968,8,6-10
Thibaut,ritual,NAC,121536,8,1-5
Thibaut,Resignz,OCE,80555,9,1-5
Thibaut,Threats,NAC,104347,10,16-20
Thibaut,E36 Stainだゾ,ASIA,81039,10,26-30
Thibaut,Spookz,OCE,77655,11,NEW
Thibaut,Scroll,EU,99319,12,NEW
Thibaut,むきむきぱぱ,ASIA,76015,12,31-infini
Thibaut,boltz,NAC,103904,13,6-10
Thibaut,Buyuriru,ASIA,75976,13,6-10
Thibaut,velo,NAW,71944,13,31-infini
Thibaut,p1ng,EU,91801,15,11-15
Thibaut,Teddy,ASIA,74310,15,11-15
Thibaut,Cold,NAC,92700,16,26-30
Thibaut,A1st Michaelfvǃ,ASIA,73687,16,21-25
Thibaut,Mace,OCE,71424,16,6-10
Thibaut,Nachiiri,ME,66312,16,31-infini
Thibaut,Job,ASIA,78140,17,16-20
Thibaut,Diguera,BR,73538,17,16-20
Thibaut,Danath,OCE,70036,18,31-infini
Thibaut,twitch sourcefn7,NAW,67917,19,21-25
Thibaut,cyrzr,NAW,63764,21,6-10
Thibaut,Bugha,NAC,93777,24,31-infini
Thibaut,Adapter,ME,60640,25,26-30
Thibaut,Vergo,NAC,81323,27,NEW
Thibaut,Fazer,BR,68517,27,1-5
Thibaut,Hellon,ME,60116,27,NEW
Thibaut,K1nG,BR,66151,33,21-25
Thibaut,minit,NAW,55136,33,NEW
Thibaut,larccoz,NAW,58631,34,NEW
Thibaut,Puma,ME,54659,35,16-20
Thibaut,AP defiable,NAW,53621,35,26-30
Thibaut,916Gon,BR,64757,38,6-10
Thibaut,Axadasz,BR,64503,39,11-15
Thibaut,EdRoadToGlory,BR,55931,57,31-infini
Thibaut,Persa,BR,54976,61,26-30`;

  /**
   * Parse les données CSV et les organise par équipe
   */
  public parseChampionshipData(): ChampionshipData {
    const lines = this.csvData.trim().split('\n');
    const header = lines[0];
    const dataLines = lines.slice(1);

    // Parser chaque ligne de données
    const allPlayers: Array<{pronostiqueur: string, player: FortnitePlayer}> = [];
    
    for (const line of dataLines) {
      const [pronostiqueur, joueur, region, scoreStr, rankingStr, based2024] = line.split(',');
      
      const player: FortnitePlayer = {
        name: joueur.trim(),
        region: region.trim(),
        score: parseInt(scoreStr.trim()),
        ranking: parseInt(rankingStr.trim()),
        based2024: based2024.trim()
      };

      allPlayers.push({
        pronostiqueur: pronostiqueur.trim(),
        player
      });
    }

    // Grouper par pronostiqueur
    const marcelPlayers = allPlayers.filter(p => p.pronostiqueur === 'Marcel').map(p => p.player);
    const teddyPlayers = allPlayers.filter(p => p.pronostiqueur === 'Teddy').map(p => p.player);
    const thibautPlayers = allPlayers.filter(p => p.pronostiqueur === 'Thibaut').map(p => p.player);

    // Créer les équipes
    const marcel = this.createTeam('Marcel', marcelPlayers);
    const teddy = this.createTeam('Teddy', teddyPlayers);
    const thibaut = this.createTeam('Thibaut', thibautPlayers);

    return {
      marcel,
      teddy,
      thibaut,
      totalPlayers: allPlayers.length,
      lastUpdated: new Date().toISOString()
    };
  }

  /**
   * Crée une équipe avec ses statistiques
   */
  private createTeam(pronostiqueur: string, players: FortnitePlayer[]): FortniteTeam {
    const totalScore = players.reduce((sum, player) => sum + player.score, 0);
    
    // Calculer la répartition par région
    const regionDistribution: { [region: string]: number } = {};
    players.forEach(player => {
      regionDistribution[player.region] = (regionDistribution[player.region] || 0) + 1;
    });

    return {
      pronostiqueur,
      players,
      totalScore,
      playerCount: players.length,
      regionDistribution
    };
  }

  /**
   * Calcule les statistiques par région pour toutes les équipes
   */
  public calculateRegionStats(championship: ChampionshipData): RegionStats[] {
    const regionMap = new Map<string, { count: number, totalScore: number }>();

    // Traiter toutes les équipes
    [championship.marcel, championship.teddy, championship.thibaut].forEach(team => {
      team.players.forEach(player => {
        const current = regionMap.get(player.region) || { count: 0, totalScore: 0 };
        regionMap.set(player.region, {
          count: current.count + 1,
          totalScore: current.totalScore + player.score
        });
      });
    });

    // Convertir en tableau de statistiques
    return Array.from(regionMap.entries()).map(([region, stats]) => ({
      region,
      playerCount: stats.count,
      totalScore: stats.totalScore,
      averageScore: Math.round(stats.totalScore / stats.count)
    })).sort((a, b) => b.totalScore - a.totalScore);
  }

  /**
   * Obtient les statistiques détaillées d'une équipe
   */
  public getTeamDetailedStats(team: FortniteTeam) {
    const topPlayers = [...team.players]
      .sort((a, b) => b.score - a.score)
      .slice(0, 5);

    const averageScore = Math.round(team.totalScore / team.playerCount);
    
    const regionStats = Object.entries(team.regionDistribution)
      .map(([region, count]) => ({
        region,
        count,
        percentage: Math.round((count / team.playerCount) * 100)
      }))
      .sort((a, b) => b.count - a.count);

    return {
      topPlayers,
      averageScore,
      regionStats,
      totalScore: team.totalScore,
      playerCount: team.playerCount
    };
  }

  /**
   * Valide l'intégrité des données
   */
  public validateData(championship: ChampionshipData): { isValid: boolean, errors: string[] } {
    const errors: string[] = [];

    // Vérifier le nombre total de joueurs
    const expectedTotal = 147;
    if (championship.totalPlayers !== expectedTotal) {
      errors.push(`Expected ${expectedTotal} players, found ${championship.totalPlayers}`);
    }

    // Vérifier les équipes
    const marcelCount = championship.marcel.playerCount;
    const teddyCount = championship.teddy.playerCount;
    const thibautCount = championship.thibaut.playerCount;

    if (marcelCount + teddyCount + thibautCount !== championship.totalPlayers) {
      errors.push(`Team player counts don't match total: ${marcelCount} + ${teddyCount} + ${thibautCount} ≠ ${championship.totalPlayers}`);
    }

    // Vérifier que tous les joueurs ont des scores valides
    [championship.marcel, championship.teddy, championship.thibaut].forEach(team => {
      team.players.forEach(player => {
        if (!player.name || player.score <= 0 || player.ranking <= 0) {
          errors.push(`Invalid player data: ${player.name}`);
        }
      });
    });

    return {
      isValid: errors.length === 0,
      errors
    };
  }
}